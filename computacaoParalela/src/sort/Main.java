package sort;

import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws ExecutionException {
    	Scanner scanner = new Scanner(System.in);

        System.out.print("Digite o número de threads para a execução: ");
        int numThreads = scanner.nextInt();

        int[] dataSizes = {1000, 5000, 10000, 50000, 100000};
        String fileName = "resultados.csv";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Algoritmo, Tamanho do array, Tempo de execucao (ms), Threads\n");
            System.out.println("Iniciando a execução dos algoritmos...");

            for (int size : dataSizes) {
                int[] data = generateRandomArray(size);
                System.out.println("Executando para o tamanho do array: " + size + "\n");

                // Sequencial Bubble Sort
                long start = System.nanoTime();
                BubbleSort.bubbleSort(data.clone());
                long end = System.nanoTime();
                writer.write("BubbleSort, " + size + ", " + (end - start) + ", 1\n");
                System.out.println("BubbleSort concluído para " + size + " elementos.");

                // Paralelo Bubble Sort
                start = System.nanoTime();
                ParallelBubbleSort.bubbleSortParallel(data.clone(), numThreads);
                end = System.nanoTime();
                writer.write("BubbleSort Paralelo, " + size + ", " + (end - start) + ", " + numThreads + "\n");
                System.out.println("BubbleSort Paralelo concluído para " + size + " elementos.");

                // Sequencial Selection Sort
                start = System.nanoTime();
                SelectionSort.selectionSort(data.clone());
                end = System.nanoTime();
                writer.write("SelectionSort, " + size + ", " + (end - start) + ", 1\n");
                System.out.println("SelectionSort concluído para " + size + " elementos.");

                // Paralelo Selection Sort
                start = System.nanoTime();
                ParallelSelectionSort.selectionSortParallel(data.clone(), numThreads);
                end = System.nanoTime();
                writer.write("SelectionSort Paralelo, " + size + ", " + (end - start) + ", " + numThreads + "\n");
                System.out.println("SelectionSort Paralelo concluído para " + size + " elementos.");
            }

            System.out.println("Execução concluída. Resultados gravados em " + fileName);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static int[] generateRandomArray(int size) {
        Random rand = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = rand.nextInt();
        }
        return array;
    }
}

// Implementação Bubble Sort Sequencial
class BubbleSort {
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        boolean swapped;

        for (int i = 0; i < n - 1; i++) {
            swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }
            if (!swapped) break;
        }
    }
}

// Implementação Bubble Sort Paralelo
class ParallelBubbleSort {

    public static void bubbleSortParallel(int[] array, int numThreads) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int n = array.length;
        boolean sorted = false;

        for (int i = 0; i < n && !sorted; i++) {
            CountDownLatch latch = new CountDownLatch(numThreads); // Sincronizador
            boolean isEvenPhase = (i % 2 == 0);

            for (int j = 0; j < numThreads; j++) {
                final int threadIndex = j;
                executor.submit(() -> {
                    if (isEvenPhase) {
                        // Fase par: Troca nos índices pares
                        for (int k = threadIndex * 2; k < n - 1; k += 2 * numThreads) {
                            if (array[k] > array[k + 1]) {
                                trocar(array, k, k + 1);
                            }
                        }
                    } else {
                        // Fase ímpar: Troca nos índices ímpares
                        for (int k = threadIndex * 2 + 1; k < n - 1; k += 2 * numThreads) {
                            if (array[k] > array[k + 1]) {
                                trocar(array, k, k + 1);
                            }
                        }
                    }
                    latch.countDown();
                });
            }

            latch.await();

            // Verifica se está ordenado
            sorted = true;
            for (int k = 0; k < n - 1; k++) {
                if (array[k] > array[k + 1]) {
                    sorted = false;
                    break;
                }
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private static void trocar(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}


// Implementação Selection Sort Sequencial
class SelectionSort {
    public static void selectionSort(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[j] < arr[minIdx]) {
                    minIdx = j;
                }
            }
            int temp = arr[minIdx];
            arr[minIdx] = arr[i];
            arr[i] = temp;
        }
    }
}

// Implementação Selection Sort Paralelo
class ParallelSelectionSort {
    public static void selectionSortParallel(int[] arr, int numThreads) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int n = arr.length;

        for (int i = 0; i < n - 1; i++) {
            AtomicInteger minIdx = new AtomicInteger(i);
            CountDownLatch latch = new CountDownLatch(numThreads);

            for (int j = 0; j < numThreads; j++) {
                final int start = i + 1 + j;
                executor.submit(() -> {
                    for (int k = start; k < n; k += numThreads) {
                        if (arr[k] < arr[minIdx.get()]) {
                            minIdx.set(k);
                        }
                    }
                    latch.countDown();
                });
            }
            latch.await();

            // Swap do elemento mínimo encontrado
            int temp = arr[minIdx.get()];
            arr[minIdx.get()] = arr[i];
            arr[i] = temp;
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}


// DESCOMENTE ABAIXO PARA A VERSÃO DOS ALGORITMOS INSERTIONORT E MERGESORT

//package sort;
//
//import java.util.Scanner;
//import java.util.concurrent.*;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.Random;
//
//public class Main {
//    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.print("Digite o número de threads para a execução: ");
//        int numThreads = scanner.nextInt();
//
//        int[] dataSizes = {1000, 5000, 10000, 50000, 100000};
//        String fileName = "resultados.csv";
//
//        try (FileWriter writer = new FileWriter(fileName)) {
//            writer.write("Algoritmo, Tamanho do array, Tempo de execucao (ms), Threads\n");
//            System.out.println("Iniciando a execução dos algoritmos...");
//
//            for (int size : dataSizes) {
//                int[] data = generateRandomArray(size);
//                System.out.println("Executando para o tamanho do array: " + size + "\n");
//
//                // Sequencial Insertion Sort
//                long start = System.nanoTime();
//                InsertionSort.insertionSort(data.clone());
//                long end = System.nanoTime();
//                writer.write("InsertionSort," + size + "," + ((end - start) / 1_000_000) + ",1\n");
//                System.out.println("InsertionSort concluído para " + size + " elementos.");
//
//                // Paralelo Insertion Sort
//                start = System.nanoTime();
//                ParallelInsertionSort.insertionSortParallel(data.clone(), numThreads);
//                end = System.nanoTime();
//                writer.write("InsertionSort Paralelo," + size + "," + ((end - start) / 1_000_000) + "," + numThreads + "\n");
//                System.out.println("InsertionSort Paralelo concluído para " + size + " elementos.");
//
//                // Sequencial Merge Sort
//                start = System.nanoTime();
//                MergeSort.mergeSort(data.clone());
//                end = System.nanoTime();
//                writer.write("MergeSort," + size + "," + ((end - start) / 1_000_000) + ",1\n");
//                System.out.println("MergeSort concluído para " + size + " elementos.");
//                
//                // Paralelo Merge Sort
//                start = System.nanoTime();
//                ParallelMergeSort.mergeSortParallel(data.clone(), numThreads);
//                end = System.nanoTime();
//                writer.write("MergeSort Paralelo," + size + "," + ((end - start) / 1_000_000) + "," + numThreads + "\n");
//                System.out.println("MergeSort Paralelo concluído para " + size + " elementos.");
//            }
//
//            System.out.println("Execução concluída. Resultados gravados em " + fileName);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            scanner.close();
//        }
//    }
//
//    private static int[] generateRandomArray(int size) {
//        Random rand = new Random();
//        int[] array = new int[size];
//        for (int i = 0; i < size; i++) {
//            array[i] = rand.nextInt(10000); // Exemplo: números entre 0 e 9999
//        }
//        return array;
//    }
//}
//
//// Implementação Insertion Sort Sequencial
//class InsertionSort {
//    public static void insertionSort(int[] arr) {
//        for (int i = 1; i < arr.length; i++) {
//            int key = arr[i];
//            int j = i - 1;
//            while (j >= 0 && arr[j] > key) {
//                arr[j + 1] = arr[j];
//                j--;
//            }
//            arr[j + 1] = key;
//        }
//    }
//}
//
////Implementação Insertion Sort Paralelo
//class ParallelInsertionSort {
// public static void insertionSortParallel(int[] arr, int numThreads) {
//     ForkJoinPool pool = new ForkJoinPool(numThreads);
//     ParallelInsertionSortTask task = new ParallelInsertionSortTask(arr);
//     pool.invoke(task);
// }
//
// private static class ParallelInsertionSortTask extends RecursiveTask<int[]> {
//     private final int[] arr;
//
//     ParallelInsertionSortTask(int[] arr) {
//         this.arr = arr;
//     }
//
//     @Override
//     protected int[] compute() {
//         if (arr.length < 2) {
//             return arr; // Retorna o array se já estiver ordenado
//         }
//
//         int mid = arr.length / 2;
//         int[] left = Arrays.copyOfRange(arr, 0, mid);
//         int[] right = Arrays.copyOfRange(arr, mid, arr.length);
//
//         // Cria subtarefas para ordenação das partes
//         ParallelInsertionSortTask leftTask = new ParallelInsertionSortTask(left);
//         ParallelInsertionSortTask rightTask = new ParallelInsertionSortTask(right);
//
//         // Fork as subtarefas
//         leftTask.fork();
//         int[] rightResult = rightTask.compute(); // Processa a tarefa da direita imediatamente
//         int[] leftResult = leftTask.join(); // Espera a tarefa da esquerda concluir
//
//         // Mescla os resultados
//         return merge(leftResult, rightResult);
//     }
//
//     private int[] merge(int[] left, int[] right) {
//         int[] merged = new int[left.length + right.length];
//         int i = 0, j = 0, k = 0;
//
//         while (i < left.length && j < right.length) {
//             if (left[i] <= right[j]) {
//                 merged[k++] = left[i++];
//             } else {
//                 merged[k++] = right[j++];
//             }
//         }
//         while (i < left.length) {
//             merged[k++] = left[i++];
//         }
//         while (j < right.length) {
//             merged[k++] = right[j++];
//         }
//
//         return merged; // Retorna o array mesclado
//     }
// }
//}
//
//
//// Implementação Merge Sort Sequencial
//class MergeSort {
//    public static void mergeSort(int[] arr) {
//        if (arr.length < 2) return;
//
//        int mid = arr.length / 2;
//        int[] left = new int[mid];
//        int[] right = new int[arr.length - mid];
//
//        System.arraycopy(arr, 0, left, 0, mid);
//        System.arraycopy(arr, mid, right, 0, arr.length - mid);
//
//        mergeSort(left);
//        mergeSort(right);
//        merge(arr, left, right);
//    }
//
//    private static void merge(int[] arr, int[] left, int[] right) {
//        int i = 0, j = 0, k = 0;
//
//        while (i < left.length && j < right.length) {
//            if (left[i] <= right[j]) {
//                arr[k++] = left[i++];
//            } else {
//                arr[k++] = right[j++];
//            }
//        }
//        while (i < left.length) {
//            arr[k++] = left[i++];
//        }
//        while (j < right.length) {
//            arr[k++] = right[j++];
//        }
//    }
//}
//
//// Implementação Merge Sort Paralelo usando ForkJoinPool
//class ParallelMergeSort {
//    public static void mergeSortParallel(int[] arr, int numThreads) {
//        ForkJoinPool pool = new ForkJoinPool(numThreads);
//        pool.invoke(new MergeSortTask(arr));
//    }
//
//    private static class MergeSortTask extends RecursiveAction {
//        private final int[] arr;
//
//        MergeSortTask(int[] arr) {
//            this.arr = arr;
//        }
//
//        @Override
//        protected void compute() {
//            if (arr.length < 2) return;
//
//            int mid = arr.length / 2;
//            int[] left = new int[mid];
//            int[] right = new int[arr.length - mid];
//
//            System.arraycopy(arr, 0, left, 0, mid);
//            System.arraycopy(arr, mid, right, 0, arr.length - mid);
//
//            invokeAll(new MergeSortTask(left), new MergeSortTask(right));
//            merge(arr, left, right);
//        }
//
//        private void merge(int[] arr, int[] left, int[] right) {
//            int i = 0, j = 0, k = 0;
//
//            while (i < left.length && j < right.length) {
//                if (left[i] <= right[j]) {
//                    arr[k++] = left[i++];
//                } else {
//                    arr[k++] = right[j++];
//                }
//            }
//            while (i < left.length) {
//                arr[k++] = left[i++];
//            }
//            while (j < right.length) {
//                arr[k++] = right[j++];
//            }
//        }
//    }
//}
