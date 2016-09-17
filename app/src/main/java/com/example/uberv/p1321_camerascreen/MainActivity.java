package com.example.uberv.p1321_camerascreen;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

// ACHTUNG!
// ВРУЧНУЮ ДАТЬ PERMISSION ПРИЛОЖЕНИЮ НА ИСПОЛЬЗОВАНИЕ КАМЕРЫ!!!

// http://startandroid.ru/ru/uroki/vse-uroki-spiskom/264-urok-132-kamera-vyvod-izobrazhenija-na-ekran-obrabotka-povorota.html

public class MainActivity extends Activity {

    SurfaceView sv;
    SurfaceHolder holder;
    HolderCallback holderCallback;
    Camera camera;

    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Настраиваем прилоежние так, чтобы:
        // 1. Оно было без загаловка
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 2. в полный экран
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        // setContentView() нельзя вызывать до requestFeature()
        setContentView(R.layout.activity_main);

        // изображение камеры будет выводиться на SurfaceView, который и создан для целей рисования на нём
        sv = (SurfaceView) findViewById(R.id.surfaceView);
        // получаем его holder
        holder = sv.getHolder();

        // через этот callback наш holder будет сообщать нам о состоянии SurfaceView
        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // получаем доступ к камере
        // camera = Camera.open(CAMERA_ID); // открыть камеру с таким-то ид
        camera = Camera.open(); // без ID - задняя камера

        // назначем колбэк для каждого кадра превью
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                // TODO УБЕРИ ПОТОМ. ЕБАШИТ ПО КОНСОЛИ ПИЗДЕЦ
                Log.d("Camera","callback");
            }
        });

        // настраиваем размер surface
        setPreviewSize(FULL_SCREEN);
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance

        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @Override
    protected void onPause() {
        super.onPause();
        // освобождаем камеру
        if (camera != null)
            camera.release();
        camera = null;
    }

    /** Callback, через который holder будет сообщать нам о состоянии Surface */
    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // surfaceCreated – surface создан. Мы можем дать камере объект holder
            // с помощью метода setPreviewDisplay и начать транслировать
            // изображение методом startPreview.
            try {
                // назначить view, на который рисовать превью
                camera.setPreviewDisplay(holder);
                // начать рисовать превью
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            // surfaceChanged – был изменен формат или размер surface.

            // В этом случае мы останавливаем просмотр (stopPreview),
            camera.stopPreview();
            // настраиваем камеру с учетом поворота устройства
            setCameraDisplayOrientation(CAMERA_ID);
            try {
                // и снова запускаем просмотр.
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // surface более недоступен. Не используем этот метод.
        }

    }

    /** Настроить размер surface
     * определяем размеры surface с учетом экрана и изображения с камеры,
     * чтобы картинка отображалась с верным соотношением сторон и на весь экран. */
    void setPreviewSize(boolean fullScreen) {

        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        // что больше, ширина или высота?
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF превью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            // ширина и высота меняются местами
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            /*  если обратиться ко второму варианту, то матрица поняла,
            что ей надо будет умножить стороны объекта на 1.57,
            т.е. на меньшее из отношений длин и широт */
            /* берет на вход два RectF. И вычисляет, какие преобразования надо сделать,
             чтобы первый втиснуть во второй */
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            // даём понять, что в данном случае ей будет передан не rectDisplay, а rectPreview
            // т.е. операцию она проводить будет над ВТОРЫМ обьектом вместо первого
            /* например, если по подсчёт надо умножить первый обьект на 2, то при вызове инверт()
            * она поймёт, что надо второй обьект поделить на два */
            matrix.invert(matrix);
        }
        // преобразование
        // матрица уже знает, какие преобразования ей надо сделать с обьектом
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        sv.getLayoutParams().height = (int) (rectPreview.bottom);
        sv.getLayoutParams().width = (int) (rectPreview.right);
    }

    /** Метод, который будет вращать превью в зависимости от ориентации устройства */
    void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        // ФАКТ: в смартфонах камера повёрнута на 90 градусов
        // Её нормальная ориентация совпадает с горизонтальной ориентацией устройства
        // Чтобы и в превью и на экране ширина получалась больше высоты

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        /* Чтобы узнать итоговый поворот камеры по часовой в пространстве –
        надо сложить поворот устройства по часовой и CameraInfo.orientation.
        Это для задней камеры. А для передней – надо CameraInfo.orientation вычесть,
        потому что она смотрит в нашу сторону. И все, что для нас по часовой, для нее - против. */

        // degrees - кол-во градусов, на которые повернуто устройство против часовой
        // => чтобы узнать на сколько повернуто против часовой - вычитаем их из 360
        degrees=360-degrees;

        // задняя камера
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            // добавляем встроенный поворт камеры
            result = degrees + info.orientation;
        } else
            // передняя камера
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                // тут отнимаем, ибо переднаяя камера- против часовой
                result = degrees - info.orientation;
                result += 360; // чтобы не получилось отрицательно число (особенность передней камеры)
            }
        // Определяем итоговое кол-во градусов в пределах от 0 до 360, вычисляя остаток от деления на 360.
        result = result % 360;

        // повернуть камеру на result градусов по часовой стрелке
        camera.setDisplayOrientation(result);
    }
}
