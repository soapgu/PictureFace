package com.space365.utility;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

public class QREncode {
    /**
     *
     * @param contents 内容
     * @param _width 宽
     * @param _height 高
     * @param _margin 边距
     * @return 二维码图片
     */
    public static Bitmap encodeAsBitmap(String contents, int _width, int _height, int _margin) {
        try {

            if (contents == null) {
                return null;
            }
            Map<EncodeHintType, Object> hints = null;
            String encoding = guessAppropriateEncoding(contents);
            if (encoding != null) {
                hints = new EnumMap<>(EncodeHintType.class);
                hints.put(EncodeHintType.CHARACTER_SET, encoding);
                hints.put(EncodeHintType.MARGIN, 1);
            }
            BitMatrix result;
            try {
                result = new MultiFormatWriter().encode(contents, BarcodeFormat.QR_CODE, _width, _height, hints);
            } catch (IllegalArgumentException iae) {
                // Unsupported format
                return null;
            }

            boolean isFirstBlackPoint = false;
            int width = result.getWidth();
            int height = result.getHeight();
            int startX = 0;
            int startY = 0;
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                    if (!isFirstBlackPoint && result.get(x, y)) {
                        isFirstBlackPoint = true;
                        startX = x;//二维码自带一个白色边框，边框内左上角是黑点，记录左上角的坐标点
                        startY = y;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            int left = startX - _margin;
            int top = startY - _margin;
            if (left < 0 || top < 0) {
                return bitmap;
            } else {
                int newWidth = width - left * 2;
                int newHeight = height - top * 2;
                return cropBitmapCustom(bitmap, left, top, newWidth, newHeight);
            }
        } catch (WriterException exception) {
            return null;
        }
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    private static Bitmap cropBitmapCustom(Bitmap srcBitmap, int firstPixelX, int firstPixelY, int needWidth, int needHeight) {
        if (firstPixelX + needWidth > srcBitmap.getWidth()) {
            needWidth = srcBitmap.getWidth() - firstPixelX;
        }
        if (firstPixelY + needHeight > srcBitmap.getHeight()) {
            needHeight = srcBitmap.getHeight() - firstPixelY;
        }

        Bitmap cropBitmap = Bitmap.createBitmap(srcBitmap, firstPixelX, firstPixelY, needWidth, needHeight);
        srcBitmap.recycle();

        return cropBitmap;
    }
}
