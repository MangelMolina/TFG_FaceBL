#include <jni.h>
#include <unistd.h>
#include <omp.h>
#include <fstream>
#include <opencv2/imgproc/types_c.h>
#include "opencv2/core.hpp"
#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/imgproc.hpp"

using namespace cv;

//Función para asignar a las hebras creadas en java un procesador que se usa en el main activity
extern "C" JNIEXPORT
void JNICALL Java_com_example_javacvtest_MainActivity_asignarAfinidad(JNIEnv *env, jobject,
                                                                      jintArray procesador){

    cpu_set_t mascara; //Variable de CPU que le pasare a sched_setaffinity, indica las CPUs
                       //en las que se puede ejecutar

    jint * procesadores = env->GetIntArrayElements(procesador, NULL);

    CPU_ZERO(&mascara); //Vaciamos de todas las cpus (No hay ninguna CPU en el conjunto)
    for(int i = 0; i < env->GetArrayLength(procesador); i++){
        CPU_SET(procesadores[i],&mascara); //Metemos en el set de las CPUs la cpu con el mismo numero
                                           //que le pasamos a la funcion
    }

    sched_setaffinity(gettid(), sizeof(mascara), &mascara); //Le asignamos el conjunto de CPUS al proceso
                                                            //con el thread id actual, de manera que cada hebra tendra
                                                            //uno distino y cada una hará su propia asignacion.
}

//Función para asignar a las hebras creadas en java un procesador que se usa en la clase Hebra
extern "C" JNIEXPORT
void JNICALL Java_com_example_javacvtest_Hebra_asignarAfinidad(JNIEnv *env, jobject,
                                                                      jintArray procesador){

    cpu_set_t mascara; //Variable de CPU que le pasare a sched_setaffinity, indica las CPUs
    //en las que se puede ejecutar

    jint * procesadores = env->GetIntArrayElements(procesador, NULL);

    CPU_ZERO(&mascara); //Vaciamos de todas las cpus (No hay ninguna CPU en el conjunto)
    for(int i = 0; i < env->GetArrayLength(procesador); i++){
        CPU_SET(procesadores[i],&mascara); //Metemos en el set de las CPUs la cpu con el mismo numero
        //que le pasamos a la funcion
    }

    sched_setaffinity(gettid(), sizeof(mascara), &mascara); //Le asignamos el conjunto de CPUS al proceso
    //con el thread id actual, de manera que cada hebra tendra
    //uno distino y cada una hará su propia asignacion.
}

//Funcion que devuelve el procesador en el que se ejecuta la hebra que llama a esta funcion.
extern "C" JNIEXPORT
jint JNICALL Java_com_example_javacvtest_MainActivity_getAfinidad(JNIEnv *env, jobject){
    //devuelvo la CPU en la se ejecuta despues de modificarlo
    return sched_getcpu();
}