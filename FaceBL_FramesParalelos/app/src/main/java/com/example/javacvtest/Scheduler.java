package com.example.javacvtest;

//1 little:         2-3 FPS y 835-850 mAh
//1 big:            3-4 FPS y 870-890 mAh
//2 little:         4-5 FPS y 975-1000 mAh
//1 little y 1 big: 5-6 FPS y 1040-1045 mAh
//2 big:            7-9 FPS y 1045-1055 mAh
//3 little:         8-10 FPS y 1120-1190 mAh
//2 little y 1 big: 11-12 FPS


import android.util.Log;

public class Scheduler {
    //Los FPS que se desean alcanzar
    private int limiteFPS = 15;

    //Numero de cores big
    private int numCoresBig = 2;

    //Numero de cores big que se estan usando
    private int numCoresBigUtilizando = 2;

    //Numero de cores little
    private int numCoresLittle;

    //Numero de cores little que se estan usando
    private int numCoresLittleUtilizando;

    //El vector con los numeros de los cores que van a ser
    //usados por la aplicación (tiene el mismo formato que
    //asignarAfinidad).
    private int[] coresUtilizables;

    /**
     * Constructor del scheduler
     */
    public Scheduler(){
        //Asignamos el numero de procesadores disponibles.
        int numCoresTotales = Runtime.getRuntime().availableProcessors();
        numCoresLittle = numCoresTotales-2-numCoresBig;

        if(numCoresTotales < 3){    //Si tiene menos de 3 cores, el core
                                    //que ejecuta será el 2 (Hay un procesador
                                    //especifico para el main y otro para llamar
                                    //al scheduler cada cierto tiempo)
            coresUtilizables = new int[1];
            coresUtilizables[0] = 2;
        }
        else{   //Si tiene más, asignamos todos para empezar.
            coresUtilizables = new int[numCoresTotales - 2];
            numCoresLittleUtilizando = numCoresTotales-2 - numCoresBig;
            numCoresBigUtilizando = numCoresBig;
            for(int i = 0; i < numCoresTotales-2; i++){
                coresUtilizables[i] = i+2;
            }
        }
    }

    /**
     * Utiliza los FPS de la aplicacion para calcular si se necesita mas o menos
     * potencia de calculo, en esos casos hace los cambios necesarios.
     * @param FPS: FPS actuales de la aplicacion.
     */
    public void calcularCoresUtilizables(int FPS){
        if(FPS > limiteFPS+2 ){ //Si hay más FPS del limite, reducimos la potencia.

            //Si tenemos 4 cores little y 2 big, le quito uno big porque no
            //hay mas little por los que reemplazarlos.
            if(numCoresLittleUtilizando == 4 && numCoresBigUtilizando == 2){
                numCoresLittleUtilizando--;
            }
            else if(numCoresBigUtilizando > 0){      //Si hay uno o dos cores big, lo cambio por uno little
                numCoresLittleUtilizando++;
                numCoresBigUtilizando--;
            }
            else{
                if(numCoresLittleUtilizando > 2){       //Si no hay cores big y mas de uno little,
                    numCoresLittleUtilizando -= 3;      //reduzco sustituyo 3 little por dos big
                    numCoresBigUtilizando+=2;
                }
                else if(numCoresLittleUtilizando > 1){  //Si no hay cores big y hay un core little
                    numCoresLittleUtilizando -= 2;      //reduzco sustituyo 2 little por uno big
                    numCoresBigUtilizando++;
                }
            }

        }
        else if(FPS < limiteFPS-2){ //Si hay menos FPS del limite, añadimos potencia.

            //Si no hemos llegado al limite de cores big y hay cores little,
            //cambio un little por un big
            if(numCoresBigUtilizando < numCoresBig && numCoresLittleUtilizando > 0){
                numCoresLittleUtilizando--;
                numCoresBigUtilizando++;
            }
            //Si el numero de cores little es menor que 1, paso los cores big a littles y añado uno extra
            else if(numCoresLittleUtilizando <= 1){
                numCoresLittleUtilizando += numCoresBigUtilizando+1;
                numCoresBigUtilizando = 0;
            } else if(numCoresLittleUtilizando == 2){
                numCoresLittleUtilizando = numCoresLittle;
                numCoresBigUtilizando = 1;
            }
            else if(numCoresLittleUtilizando == 3){
                numCoresLittleUtilizando = numCoresLittle;
                numCoresBigUtilizando = numCoresBig;
            }

        }

        //Añado el numero de cores little que tendre
        for(int i = 0; i < numCoresLittleUtilizando; i++){
            coresUtilizables[i] = i+2;
        }

        //Añado el numero de cores big que tendre
        for(int i = 0; i < numCoresBigUtilizando; i++){
            coresUtilizables[i+numCoresLittleUtilizando] = i+numCoresLittle+2;
        }
    }

    /**
     * Metodo que devuelve los cores utilizables, primero los calcula.
     * @param FPS: FPS actuales de la aplicacion.
     * @return Array de enteros con los numeros de los procesadores incluidos.
     */
    public int[] getCoresUtilizables(int FPS){
        //Calculo los cores utilizables.
        calcularCoresUtilizables(FPS);

        //Creo un array con la longitud exacta y lo devuelvo.
        int [] devolver = new int[numCoresLittleUtilizando+numCoresBigUtilizando];
        for(int i = 0; i < numCoresLittleUtilizando+numCoresBigUtilizando; i++){
            devolver[i] = coresUtilizables[i];
        }

        return devolver;
    }
}
