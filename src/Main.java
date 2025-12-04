import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    public static void  main(String[] args) {
        int port = 5000;
        File file = new File("registros.txt");
        HashMap<String, ArrayList<Registro>> mapRegistros = new HashMap<>();


        try (BufferedReader leer = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = leer.readLine()) != null) {
                String[] datos = line.split(" ");

                Registro registro = new Registro(datos[0], datos[1], datos[2]);

                if (mapRegistros.containsKey(datos[0])) {
                    ArrayList<Registro> lista = mapRegistros.get(datos[0]);
                    lista.add(registro);
                } else {
                    ArrayList<Registro> listaRegistros = new ArrayList<Registro>();
                    listaRegistros.add(registro);
                    mapRegistros.put(datos[0], listaRegistros);
                }

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        try (ServerSocket serverSocket = new ServerSocket(port)) {

            Socket cliente = serverSocket.accept();

            BufferedReader reader = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);

            String mensaje;
            while ((mensaje = reader.readLine()) != null) {
                try {

                    if (mensaje.equalsIgnoreCase("EXIT")) {
                        break;
                    }

                    String[] comandos = mensaje.split(" ");
                    String comando = comandos[0].toUpperCase();

                    switch (comando) {

                        case "LOOKUP" -> {
                            String resultado = resolverDominio(comandos, mapRegistros);
                            salida.println(resultado);
                        }

                        default -> salida.println("400 Bad request");
                    }

                } catch (Exception e) {
                    salida.println("500 Server error");
                }
            }

            cliente.close();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


        public static String resolverDominio(String[] comandos, HashMap<String, ArrayList<Registro>> mapRegistros){

        String dominio = comandos[2];
        String operacion = comandos[1];
        String resultado = "";


        if (comandos.length != 3 || !comandos[0].equals("LOOKUP")) {
            return "400 Bad request";
        }

            if (!(operacion.equalsIgnoreCase("A") || operacion.equalsIgnoreCase("MX") || operacion.equalsIgnoreCase("CNAME"))) {
                return "400 Bad request";
            }

        if (mapRegistros.get(dominio) != null){
            ArrayList<Registro> listaRegistros = mapRegistros.get(dominio);

            for (Registro r: listaRegistros){
                if(r.getTipo().equals(operacion)){
                    String ip =  r.getIp();
                    resultado = "200 " + ip;
                }
            }
        } else{
            resultado =  "404 NOT FOUND";

        }

        return resultado;

    }

}
