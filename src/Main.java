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
                            salida.println(resolverDominio(comandos, mapRegistros));
                        }

                        case "LIST" -> {
                           salida.println(listarRegistros(comandos, mapRegistros));
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

        if (comandos.length != 3 || !comandos[0].equalsIgnoreCase("LOOKUP")) {
            return "400 Bad request";
        }

        String dominio = comandos[2];
        String tipo = comandos[1];
        StringBuilder resultado = new StringBuilder();

        if (!(tipo.equalsIgnoreCase("A") || tipo.equalsIgnoreCase("MX") || tipo.equalsIgnoreCase("CNAME"))) {
                return "400 Bad request";
        }

        ArrayList<Registro> listaDeRegistros = mapRegistros.get(dominio);
        if (listaDeRegistros == null) {
            return "404 Not Found";
        }

        for (Registro r : listaDeRegistros) {
            if (r.getTipo().equalsIgnoreCase(tipo)) {
                resultado.append("200 ").append(r.getValor()).append("\n");
            }
        }

        if (resultado.isEmpty()){
            return "404 NOT FOUND";
        }

        return resultado.toString().trim();

    }

    public static String listarRegistros(String[] comandos, HashMap<String, ArrayList<Registro>> mapRegistros) {

        if (comandos.length != 1 || !comandos[0].equalsIgnoreCase("LIST")) {
            return "400 Bad request";
        }

        StringBuilder registros = new StringBuilder();
        registros.append("150 Inicio listado").append("\n");

        for (String dominio : mapRegistros.keySet()) {
            ArrayList<Registro> listaRegistros = mapRegistros.get(dominio);

            for (Registro r : listaRegistros) {
                registros.append(dominio)
                        .append(" ")
                        .append(r.getTipo())
                        .append(" ")
                        .append(r.getValor())
                        .append("\n");
            }
        }

        registros.append("226 Fin listado");
        return registros.toString();
    }



}
