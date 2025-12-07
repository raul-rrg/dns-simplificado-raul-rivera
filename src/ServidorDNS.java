import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class ServidorDNS {

    static final int PUERTO = 5000;
    static final int MAX_CLIENTES = 5;
    static int numeroActualDeClientes = 0;

    public static void  main(String[] args) {

        HashMap<String, ArrayList<Registro>> diccionario = new HashMap<>();

        File ficheroRegistros = new File("registros.txt");
        leerFicheroRegistros(ficheroRegistros, diccionario);


        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {

            while (true) {

                Socket cliente = serverSocket.accept();

                PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);

                if (numeroActualDeClientes < MAX_CLIENTES) {
                    Thread thread = new Thread(new HiloCliente(cliente, diccionario));
                    agregarCliente();
                    thread.start();
                } else {
                    salida.println("500 Server Error - Clientes Máximos Alcanzados");
                    cliente.close();
                }

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }



    public static synchronized void quitarCliente() {
        if (numeroActualDeClientes > 0) {
            numeroActualDeClientes--;
            System.out.println("Cliente desconectado, total clientes: " + numeroActualDeClientes);
        }
    }
    public static synchronized void agregarCliente(){
        numeroActualDeClientes++;
        System.out.println("Cliente conectado, total clientes: " + numeroActualDeClientes);

    }

    private static  void leerFicheroRegistros(File file, HashMap<String, ArrayList<Registro>> diccionario) {
        try (BufferedReader leer = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = leer.readLine()) != null) {
                String[] datos = line.split(" ");

                Registro registro = new Registro(datos[0], datos[1], datos[2]);

                if (diccionario.containsKey(datos[0])) {
                    diccionario.get(datos[0]).add(registro);
                } else {
                    ArrayList<Registro> lista = new ArrayList<>();
                    lista.add(registro);
                    diccionario.put(datos[0], lista);
                }
            }
        } catch (Exception e) {
            System.out.println("500 Server error - Error al leer el fichero: " + e.getMessage());
        }
    }

}
