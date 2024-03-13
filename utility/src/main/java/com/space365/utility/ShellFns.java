package com.space365.utility;

import com.orhanobut.logger.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellFns {
    /**
     * su静默执行命令
     * @param cmd shell 指令
     * @return 是否成功
     */
    public static synchronized boolean excuteSlient(String cmd) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        int resultCode = -1;
        try {
            //静默安装需要root权限
            Logger.i("begin su execute command:%s",cmd);
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.write(cmd.getBytes());
            os.writeBytes("\n");
            os.writeBytes("exit\n");
            os.flush();
            //执行命令
            resultCode = process.waitFor();
            Logger.i( "wait result code:%s",resultCode );
            //获取返回结果
            successMsg = new StringBuilder();
            errorMsg = new StringBuilder();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            Logger.e( e, "error on execute client" );
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //显示结果
        String str = String.format("执行成功消息：%s\n错误消息: %s", successMsg, errorMsg);
        Logger.i(str);
        return resultCode == 0;
    }

    public synchronized static boolean exec(String command) {
        Logger.i( "normal execute command:%s", command);

        Process process = null;

        try {
            process = Runtime.getRuntime().exec(command);

            BufferedReader std = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            int resultCode = process.waitFor();


            if( resultCode == 0 ){
                return true;
            }else {
                StringBuilder successMsg = new StringBuilder();
                StringBuilder errorMsg = new StringBuilder();
                String s;
                while ((s = std.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = error.readLine()) != null) {
                    errorMsg.append(s);
                }
                Logger.w("执行成功消息：%s\n错误消息: %s", successMsg, errorMsg);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            // ignore
            return false;
        } finally {
            try {
                assert process != null;
                process.destroy();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
