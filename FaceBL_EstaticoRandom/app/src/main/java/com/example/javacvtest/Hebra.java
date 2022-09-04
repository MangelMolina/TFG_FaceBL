package com.example.javacvtest;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.util.concurrent.Callable;

public class Hebra implements Callable<Rect[]> {
    private Mat faceMat;
    private CascadeClassifier cascade;
    private boolean esOjos = false;
    private boolean esBocas = false;
    private boolean esNarices = false;
    private int[] coresEjecucion;

    public Hebra(CascadeClassifier c, Mat mat, String detectar, int [] cores){
        faceMat = mat;
        cascade = c;

        if(detectar == "ojos")
            esOjos = true;
        else if(detectar == "bocas")
            esBocas = true;
        else if(detectar == "narices")
            esNarices = true;

        coresEjecucion = cores;
    }

    @Override
    public Rect[] call() throws Exception {
        asignarAfinidad(coresEjecucion);

        if (esOjos)
            return detectarOjos(faceMat);
        else if (esBocas)
            return detectarBocas(faceMat);
        else
            return detectarNarices(faceMat);
    }

    private Rect[] detectarOjos(Mat faceROI){
        double inicio = System.currentTimeMillis();

        //Creo el objeto donde se guardaran los ojos
        MatOfRect eyes = new MatOfRect();

        //Por cada cara, detecto los ojos
        cascade.detectMultiScale( faceROI, eyes, 1.1, 8,
                2, new Size(100, 100), new Size(150,150) );

        double fin = System.currentTimeMillis();
        Log.i("LogCat", "Tiempo ojo: " + (double) ((fin - inicio)));

        //Lo convierto en un array y lo devuelvo
        return eyes.toArray();
    }

    private Rect[] detectarBocas(Mat faceROI){
        double inicio = System.currentTimeMillis();

        //Creo el objeto donde se guardaran los ojos
        MatOfRect mouths = new MatOfRect();

        //Por cada cara, detecto los ojos
         cascade.detectMultiScale( faceROI, mouths, 1.1, 20,
                2, new Size(120, 120), new Size(250,250) );

        double fin = System.currentTimeMillis();
        Log.i("LogCat", "Tiempo boca: " + (double) ((fin - inicio)));

        //Lo convierto en un array y lo devuelvo
        return mouths.toArray();
    }

    private Rect[] detectarNarices(Mat faceROI){
        double inicio = System.currentTimeMillis();

        //Creo el objeto donde se guardaran los ojos
        MatOfRect noses = new MatOfRect();

        //Por cada cara, detecto los ojos
        cascade.detectMultiScale( faceROI, noses, 1.1, 20,
                2, new Size(120, 120), new Size(250,250) );

        double fin = System.currentTimeMillis();
        Log.i("LogCat", "Tiempo nariz: " + (double) ((fin - inicio)));

        //Lo convierto en un array y lo devuelvo
        return noses.toArray();
    }

    private static native void asignarAfinidad(int procesadores[]);
}
