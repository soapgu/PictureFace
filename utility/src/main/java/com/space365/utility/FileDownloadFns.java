package com.space365.utility;


import com.blankj.utilcode.constant.MemoryConstants;
import com.blankj.utilcode.util.ConvertUtils;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;

import okhttp3.ResponseBody;

public abstract class FileDownloadFns {

    //文件下载
    public static boolean writeResponseBodyToDisk(ResponseBody body, String filePath, BiConsumer<Long,Long> onProgress) {
        try {
            File file = new File(filePath);
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                Logger.i( "File Size: %f M" , ConvertUtils.byte2MemorySize( fileSize , MemoryConstants.MB ) );
                long fileSizeDownloaded = 0;

                final long total = body.contentLength();

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;

                    final long finalSum = fileSizeDownloaded;
                    //进度回调
                    onProgress.accept( finalSum , total );
                }
                Logger.i( "Download File Size: %f M" , ConvertUtils.byte2MemorySize( fileSizeDownloaded , MemoryConstants.MB ) );
                outputStream.flush();
                return true;
            } catch (IOException e) {
                Logger.e(e ,"downlaod error" );
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
