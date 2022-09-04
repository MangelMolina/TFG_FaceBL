package com.example.javacvtest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.ellipse;
import static org.opencv.imgproc.Imgproc.rectangle;

public class MainActivity extends AppCompatActivity {

    //El codigo de la peticion de los permisos
    private int REQUEST_CODE_PERMISSIONS = 101;

    //El array de String que indica los permisos que se pediran
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    //Cubre toda la pantalla, se usa para saber el tamaño total
    private TextureView textureView;

    //Es la parte de la actividad donde se ira poniendo la imagen
    //una vez sea procesada.
    private ImageView ivBitmap;

    //Es el encargado de la deteccion de las caras
    private CascadeClassifier cascadeClassifier;

    //Es el encargado de la deteccion de los ojos
    private CascadeClassifier eyeClassifier;

    //Es el encargado de la deteccion de la boca
    private CascadeClassifier mouthClassifier;

    //Es el encargado de la deteccion de la nariz
    private CascadeClassifier noseClassifier;

    //Es una instancia de la clase ImageAnalysis que se bindea a la
    //camara para que antes de devolver el frame se analice y modifique.
    //Es como un pipeline.
    private ImageAnalysis imageAnalysis;

    //Es una instancia de preview
    private Preview preview;

    //String donde se guarda el tag para sacar cosas por consola
    public static final String TAG = "LogCat";

    //Instancia de la clase FPS que mide los FPS
    private MedidorFPS fps;

    //Es el texto de la pantalla que indica los fps
    private TextView textoFPS;

    //Es el texto de la pantalla que indica los cores
    private TextView textoCores;

    //Es el objeto que permite saber los mA que se usan por hora
    private BatteryManager bm;

    //Es el texto de la pantalla que indica los mA que se usan por hora
    private TextView textoBateria;

    //Pool de hebras
    private ThreadPoolExecutor hebras = (ThreadPoolExecutor) Executors.newFixedThreadPool(6);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    initializeOpenCVDependencies();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    static {
       System.loadLibrary("javacvtest");
    }

    /**
     * Se llama a este método cuando se reanuda la aplicación, se inicializa opencv.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {    //Se inicia OpenCV y se comprueba si se inicia correctamente.
            Log.d(TAG, "Libreria OpenCV no encontrada. Usando OpenCV Manager para su inicializacion.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {    //Si no se ha iniciado correctamente
            Log.d(TAG, "Libreria OpenCV encontrada.");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * Metodo que inicializa las dependencias de OpenCV.
     */
    private void initializeOpenCVDependencies() {

        try {
            //Primero creo un nuevo archivo "cascade.xml" y abro el archivo "lbpcascade_frontalface"
            //donde se guardan los datos necesarios para la deteccion de las caras.
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "cascade.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            //Copio el contenido de "lbpcascade_frontalface" en el archivo "cascade.xml"
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            //Inicializo el CascadeClassifier, que es el encargado de detectar las caras.
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            cascadeClassifier.load(mCascadeFile.getAbsolutePath());

            //Repito el proceso anterior pero para el detector de ojos
            is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
            File eyeDir = getDir("eye", Context.MODE_PRIVATE);
            File mEyeFile = new File(eyeDir, "eye.xml");
            os = new FileOutputStream(mEyeFile);

            buffer = new byte[4096];
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            eyeClassifier = new CascadeClassifier(mEyeFile.getAbsolutePath());
            eyeClassifier.load(mEyeFile.getAbsolutePath());

            //Repito el proceso anterior pero para el detector de bocas
            is = getResources().openRawResource(R.raw.haarcascade_mcs_mouth);
            File mouthDir = getDir("mouth", Context.MODE_PRIVATE);
            File mMouthFile = new File(mouthDir, "mouth.xml");
            os = new FileOutputStream(mMouthFile);

            buffer = new byte[4096];
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mouthClassifier = new CascadeClassifier(mMouthFile.getAbsolutePath());
            mouthClassifier.load(mMouthFile.getAbsolutePath());

            //Repito el proceso anterior pero para el detector de narices
            is = getResources().openRawResource(R.raw.haarcascade_mcs_nose);
            File noseDir = getDir("nose", Context.MODE_PRIVATE);
            File mNoseFile = new File(noseDir, "nose.xml");
            os = new FileOutputStream(mNoseFile);

            buffer = new byte[4096];
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            noseClassifier = new CascadeClassifier(mNoseFile.getAbsolutePath());
            noseClassifier.load(mNoseFile.getAbsolutePath());

            //Inicio el contados de FPS
            iniciarFPS();

            if (allPermissionsGranted()) {  //Si tengo todos los permisos, inicio la cámara
                startCamera();
            } else {                        //Si no tengo los permisos, los pido.
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

    }

    /**
     * Metodo que se llama cuando se crea la actividad.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Indico que no quiero título
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Indico que quiero que se vea en pantalla completa (no sale la barra de notificaciones)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Indico que se mantenga la pantalla encendida, para que no se apague cuando no tiene uso.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        //Inicializo los componentes de la pantalla para modificarlos
        textureView = findViewById(R.id.textureView);
        ivBitmap = findViewById(R.id.ivBitmap);
        textoBateria = findViewById(R.id.bateria);
        textoFPS = findViewById(R.id.fps);
        textoCores = findViewById(R.id.cores);

        //Inicializo el objeto que servirá para actualizar la bateria.
        bm = (BatteryManager) getSystemService(BATTERY_SERVICE);

        //Muestro los cores que se estan utilizando
        textoCores.setText("Cores: 2 3 4 5 6 7");
    }

    /**
     * Metodo para activar y empezar a usar la camara.
     */
    private void startCamera() {
        //Inicializo los componentes de la camara
        CameraX.unbindAll();
        preview = setPreview();
        imageAnalysis = setImageAnalysis();
        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    /**
     * Metodo para crear un preview.
     * @return preview
     */
    private Preview setPreview() {

        //Asignamos el tamaño de la pantalla
        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        android.util.Size screen = new android.util.Size(textureView.getWidth(), textureView.getHeight());

        //Configuramos el preview
        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        //Incluimos el listener
        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });

        return preview;
    }

    /**
     * Metodo que se usa para el análisis de la imagen, donde se comprueban las caras
     * @return imageAnalysis
     */
    private ImageAnalysis setImageAnalysis() {

        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                .setImageQueueDepth(1).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                                //Analizo el frame llamando al metodo
                                AnalisisFrame();

                                //Marco que hay un FPS nuevo para que midan los FPS
                                fps.measure();

                                //Finalizo la imagen, ya que no la vuelvo a usar
                                image.close();
                    }
                });


        return imageAnalysis;

    }

    /**
     * Es el metodo que se ejecuta cuando se analiza un frame.
     */
    private void AnalisisFrame(){
        int [] hebraAnalizar = new int[1];
        hebraAnalizar[0] = 1;
        asignarAfinidad(hebraAnalizar);

        long inicioTotal = System.currentTimeMillis();

        //bitmap es la imagen que nos pasa la camara
        Bitmap bitmap = textureView.getBitmap();
        if (bitmap == null)
            return;

        //Creo un Mat nuevo donde guardaremos la imagen
        Mat mat = new Mat();

        //Convierto el bitmap a mat
        Utils.bitmapToMat(bitmap, mat);

        //Aqui lo tengo en cpp por si lo necesito.
        //analisisFrame(mat.getNativeObjAddr(), cascade);

        Mat grayScaleMat = new Mat();
        //Cambio el color RGBA a GRAY y lo guardo en grayScaleMat
        Imgproc.cvtColor(mat, grayScaleMat, Imgproc.COLOR_RGBA2GRAY);

        //Creo el objeto donde se guardaran las caras
        MatOfRect faces = new MatOfRect();

        long inicio = System.currentTimeMillis();

        //Identifico las caras dentro de la imagen en gris
        cascadeClassifier.detectMultiScale(grayScaleMat, faces, 1.3, 5, 2,
                new Size(300, 300), new Size());

        long fin = System.currentTimeMillis();
        Log.i(TAG, "Tiempo detectMultiScale: " + (double) ((fin - inicio)));

        //Guardo las caras en un array de Rect
        Rect[] faceRects = faces.toArray();

        //Por cada cara, le dibujo una elipse sobre mat
        for( int i = 0; i < faceRects.length; i++ ) {
            int numCara = i;

            Point centro = new Point(faceRects[numCara].x + faceRects[numCara].width * 0.5,
                    faceRects[numCara].y + faceRects[numCara].height * 0.5);
            ellipse(mat, centro, new Size(faceRects[numCara].width, faceRects[numCara].height), 0,
                    0, 360, new Scalar(255, 0, 255), 4, 8, 0);

            //Divido la cara en 2, de manera que se solapen un poco por si un
            //rasgo queda en medio
            Rect leftFace = faceRects[numCara].clone();
            leftFace.width /= 1.5;

            Rect rightFace = faceRects[numCara].clone();
            rightFace.width /= 1.5;
            rightFace.x = (faceRects[numCara].x+faceRects[numCara].width)-rightFace.width;

            Mat leftFaceROI = grayScaleMat.submat(leftFace);
            Mat rightFaceROI = grayScaleMat.submat(rightFace);

            //Llamo a la función de detección de ojos que me devuelve la posición de los ojos
            Rect[] leftEyesRects = new Rect[0];
            Rect[] rightEyesRects = new Rect[0];
            //Llamo a la función de detección de ojos que me devuelve la posición de los ojos
            Rect[] leftMouthsRects = new Rect[0];
            Rect[] rightMouthsRects = new Rect[0];
            //Llamo a la función de detección de ojos que me devuelve la posición de los ojos
            Rect[] leftNosesRects = new Rect[0];
            Rect[] rightNosesRects = new Rect[0];

            int[] coresHebras = new int[1];

            coresHebras[0] = 2;
            Future futureLeftEyes = hebras.submit(new Hebra(eyeClassifier, leftFaceROI,"ojos",coresHebras));
            coresHebras[0] = 3;
            Future futureRightEyes = hebras.submit(new Hebra(eyeClassifier, rightFaceROI,"ojos",coresHebras));

            coresHebras[0] = 6;
            Future futureLeftMouths = hebras.submit(new Hebra(mouthClassifier, leftFaceROI,"bocas",coresHebras));
            coresHebras[0] = 7;
            Future futureRightMouths = hebras.submit(new Hebra(mouthClassifier, rightFaceROI,"bocas",coresHebras));

            coresHebras[0] = 4;
            Future futureLeftNoses = hebras.submit(new Hebra(noseClassifier, leftFaceROI,"narices",coresHebras));
            coresHebras[0] = 5;
            Future futureRightNoses = hebras.submit(new Hebra(noseClassifier, rightFaceROI,"narices",coresHebras));


            try {
                leftEyesRects = (Rect[]) futureLeftEyes.get();
                rightEyesRects = (Rect[]) futureRightEyes.get();

                leftMouthsRects = (Rect[]) futureLeftMouths.get();
                rightMouthsRects = (Rect[]) futureRightMouths.get();

                leftNosesRects = (Rect[]) futureLeftNoses.get();
                rightNosesRects = (Rect[]) futureRightNoses.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Por cada ojo dibujo un circulo alrededor
            for (int j = 0; j < leftEyesRects.length; j++) {
                Point center = new Point(leftFace.x + leftEyesRects[j].x + leftEyesRects[j].width * 0.5,
                        leftFace.y + leftEyesRects[j].y + leftEyesRects[j].height * 0.5);
                int radius = (int) Math.round((leftEyesRects[j].width + leftEyesRects[j].height) * 0.5);
                circle(mat, center, radius, new Scalar(255, 0, 0), 4, 8, 0);
            }

            //Por cada ojo dibujo un circulo alrededor
            for (int j = 0; j < rightEyesRects.length; j++) {
                Point center = new Point(rightFace.x + rightEyesRects[j].x + rightEyesRects[j].width * 0.5,
                        rightFace.y + rightEyesRects[j].y + rightEyesRects[j].height * 0.5);
                int radius = (int) Math.round((rightEyesRects[j].width + rightEyesRects[j].height) * 0.5);
                circle(mat, center, radius, new Scalar(255, 0, 0), 4, 8, 0);
            }

            //Por cada boca dibujo un circulo alrededor
            for (int j = 0; j < leftMouthsRects.length; j++) {
                Point center = new Point(leftFace.x + leftMouthsRects[j].x + leftMouthsRects[j].width * 0.5,
                        leftFace.y + leftMouthsRects[j].y + leftMouthsRects[j].height * 0.5);
                int radius = (int) Math.round((leftMouthsRects[j].width + leftMouthsRects[j].height) * 0.5);
                circle(mat, center, radius, new Scalar(0, 255, 0), 4, 8, 0);
            }

            //Por cada boca dibujo un circulo alrededor
            for (int j = 0; j < rightMouthsRects.length; j++) {
                Point center = new Point(rightFace.x + rightMouthsRects[j].x + rightMouthsRects[j].width * 0.5,
                        rightFace.y + rightMouthsRects[j].y + rightMouthsRects[j].height * 0.5);
                int radius = (int) Math.round((rightMouthsRects[j].width + rightMouthsRects[j].height) * 0.5);
                circle(mat, center, radius, new Scalar(0, 255, 0), 4, 8, 0);
            }

            //Por cada nariz dibujo un circulo alrededor
            for (int j = 0; j < leftNosesRects.length; j++) {
                Point center = new Point(leftFace.x + leftNosesRects[j].x + leftNosesRects[j].width * 0.5,
                        leftFace.y + leftNosesRects[j].y + leftNosesRects[j].height * 0.5);
                int radius = (int) Math.round((leftNosesRects[j].width + leftNosesRects[j].height) * 0.5);
                circle(mat, center, radius, new Scalar(255, 255, 255), 4, 8, 0);
            }

            //Por cada nariz dibujo un circulo alrededor
            for (int j = 0; j < rightNosesRects.length; j++) {
                Point center = new Point(rightFace.x + rightNosesRects[j].x + rightNosesRects[j].width * 0.5,
                        rightFace.y + rightNosesRects[j].y + rightNosesRects[j].height * 0.5);
                int radius = (int) Math.round((rightNosesRects[j].width + rightNosesRects[j].height) * 0.5);
                circle(mat, center, radius, new Scalar(255, 255, 255), 4, 8, 0);
            }
        }

        //Vuelvo a convertir mat en un Bitmap para sacarlo por pantalla
        Utils.matToBitmap(mat, bitmap);

        long finTotal = System.currentTimeMillis();
        Log.i(TAG, "Tiempo total: " + (double) ((finTotal - inicioTotal)));

        //Cambio los elementos que aparecen por pantalla, que solo pueden
        //ser cambiadas desde el thread de la interfaz
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Muestro por pantalla los FPS
                textoFPS.setText("FPS: " + fps.getFPS());

                //Muestro la bateria que se esta usando actualmente
                String bateria = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)/-1000f+" mAh";
                if(!bateria.equals(textoBateria.getText().toString())){
                    textoBateria.setText(bateria);
                    Log.i("FPSMeter", bateria);
                }

                //Muestro la imagen modificada
                ivBitmap.setImageBitmap(bitmap);
            }
        });
    }

    /**
     * Metodo para transformar la matriz, dependiendo de la rotacion
     * de la imagen.
     */
    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    /**
     * Metodo que se ejecuta cuando se piden los permisos.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Metodo para ver si todos los permisos han sido concedidos
     * @return boolean
     */
    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Metodo para inicializar el medidor de FPS
     * que nos proporciona OpenCV.
     */
    private void iniciarFPS(){
        fps = new MedidorFPS();
        fps.setResolution(textureView.getWidth(), textureView.getHeight());
    }

    private static native void asignarAfinidad(int procesadores[]);
    private static native int getAfinidad();
}
