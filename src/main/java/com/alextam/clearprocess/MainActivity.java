package com.alextam.clearprocess;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AlexTam on 2015/7/29.
 */
public class MainActivity extends Activity {
    private ListView ll_main;

    private List<ProcessInfo> processList
            = new ArrayList<ProcessInfo>();
    private List<ApplicationInfo> applicationInfoList ;

    private ProcessListAdapter adapter = null;

    private Button btn_clear;

    private List<String> processNamelist = new ArrayList<String>();

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ll_main = (ListView) findViewById(R.id.lv_main);
        btn_clear = (Button) findViewById(R.id.btn_main_clear);

        getProcessList();

        btn_clear.setOnClickListener(new MyOnclick());
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }


    private class MyOnclick implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            if(v == btn_clear)
            {
                clearAllBackgroundProcess();
            }
        }
    }


    /**
     * 获取进程信息列表
     */
    private void getProcessList()
    {
        ActivityManager activityManager
                = (ActivityManager)getSystemService(ACTIVITY_SERVICE);

        //获取所有将运行中的进程
        List<ActivityManager.RunningAppProcessInfo> runningAppList
                = activityManager.getRunningAppProcesses();

        PackageManager packageManager
                = this.getPackageManager();

        //获取所有包信息
        applicationInfoList
                = packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);


        if(processList != null && processList.size() > 0)
            processList.clear();

        if(processNamelist != null && processNamelist.size() > 0)
            processNamelist.clear();

        for(ActivityManager.RunningAppProcessInfo process : runningAppList)
        {
            if(process.processName.indexOf(this.getPackageName()) < 0)
            {   //过滤本应用包名
                ProcessInfo p = new ProcessInfo();

                ApplicationInfo appInfo = getApplicationInfoByProcessName(process.processName);
                if(appInfo == null)
                {
                    //有些应用的守护进程并没有目标应用对应,此时返回null
                }
                else
                {
                    p.setLabelIcon(appInfo.loadIcon(packageManager));
                    p.setLabelName(appInfo.loadLabel(packageManager).toString());
                    p.setProcessName(appInfo.processName);

                    processNamelist.add(appInfo.processName);
                    processList.add(p);
                }
            }
        }

        if(adapter == null)
        {
            adapter = new ProcessListAdapter(MainActivity.this, processList,new ItemButtonClick());
            ll_main.setAdapter(adapter);
            ll_main.setOnItemClickListener(new MyOnItemClickListener());
        }
        else
        {
            adapter.notifyDataSetChanged();
        }

    }

    /**
     * 根据进程名获取应用信息
     * @param processNames
     * @return
     */
    private ApplicationInfo getApplicationInfoByProcessName(String processNames)
    {
        if(applicationInfoList == null || applicationInfoList.size() < 1)
            return null;

        for(ApplicationInfo applicationInfo : applicationInfoList)
        {
            if(applicationInfo.processName.equals(processNames)
                    && (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0)
                //只显示第三方的应用进程,不显示系统应用
                //要显示所有应用进程,删去(applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0 即可
                return applicationInfo;
        }
        return null;
    }

    private class MyOnItemClickListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            TextView tv_appName = (TextView) view.findViewById(R.id.tv_app_name);
            TextView tv_processName = (TextView) view.findViewById(R.id.tv_app_process_name);

            String appName = tv_appName.getText().toString();
            String processName = tv_processName.getText().toString();

            Toast.makeText(MainActivity.this, "应用: " + appName + "\n进程: " + processName,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private class ItemButtonClick implements ProcessListAdapter.processListButtonClick
    {
        String pName = null;

        @Override
        public void onButtonClick(String processName) {
            pName = processName;

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("关闭进程")
                    .setMessage("确定要关闭 " + processName+" 进程吗?")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if(pName != null)
                            {
                                ActivityManager activityManager
                                        = (ActivityManager)MainActivity.this.
                                        getSystemService(ACTIVITY_SERVICE);

                                activityManager.killBackgroundProcesses(pName);
                                getProcessList();
                            }
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

            builder.show();
        }
    }


    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == 0x1)
            {
                startClearAnim();
            }
            else if(msg.what == 0x2)
            {
                stopClearAnim();
                getProcessList();
            }
            super.handleMessage(msg);
        }
    };


    /**
     * 一键清理
     */
    private void clearAllBackgroundProcess()
    {
        mHandler.sendEmptyMessage(0x1);

        ActivityManager activityManager
                = (ActivityManager)MainActivity.this.getSystemService(ACTIVITY_SERVICE);

        if(processNamelist != null && processNamelist.size() > 0)
        {
            for(String processName : processNamelist)
            {
                activityManager.killBackgroundProcesses(processName);
            }
        }
        mHandler.sendEmptyMessageDelayed(0x2, 2000);
    }

    private void startClearAnim()
    {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("努力清理中...");
        progressDialog.show();
    }

    private void stopClearAnim()
    {
        progressDialog.dismiss();
    }


}
