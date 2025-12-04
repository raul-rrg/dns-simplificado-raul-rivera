import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    public static void  main(String[] args) {
        int port = 5000;
        File file = new File("registros.txt");
        HashMap<String, ArrayList<Registro>> diccionario = new HashMap<>();

        leerFicheroRegistros(file, diccionario);

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
                            salida.println(resolverDominio(comandos, diccionario));
                        }

                        case "LIST" -> {
                           salida.println(listarRegistros(comandos, diccionario));
                        }

                        case "REGISTER" -> {
                            salida.println(agregarRegistro(comandos, diccionario));
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



    public static String resolverDominio(String[] comandos, HashMap<String, ArrayList<Registro>> diccionario){

        if (comandos.length != 3 || !comandos[0].equalsIgnoreCase("LOOKUP")) {
            return "400 Bad request";
        }

        String dominio = comandos[2];
        String tipo = comandos[1];
        StringBuilder resultado = new StringBuilder();

        if (!(tipo.equalsIgnoreCase("A") || tipo.equalsIgnoreCase("MX") || tipo.equalsIgnoreCase("CNAME"))) {
                return "400 Bad request";
        }

        ArrayList<Registro> listaDeRegistros = diccionario.get(dominio);
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

    public static String listarRegistros(String[] comandos, HashMap<String, ArrayList<Registro>> diccionario) {

        if (comandos.length != 1 || !comandos[0].equalsIgnoreCase("LIST")) {
            return "400 Bad request";
        }

        StringBuilder registros = new StringBuilder();
        registros.append("150 Inicio listado").append("\n");

        for (String dominio : diccionario.keySet()) {
            ArrayList<Registro> listaRegistros = diccionario.get(dominio);

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

    public static String agregarRegistro(String[] comandos, HashMap<String, ArrayList<Registro>> diccionario) {

        if (comandos.length != 4 || !comandos[0].equalsIgnoreCase("REGISTER")) {
            return "400 Bad request";
        }

        String dominio = comandos[1];
        String tipo = comandos[2];
        String valor = comandos[3];

        if (!(tipo.equalsIgnoreCase("A") || tipo.equalsIgnoreCase("MX") || tipo.equalsIgnoreCase("CNAME"))) {
            return "400 Bad request";
        }

        Registro registro = new Registro(dominio, tipo, valor);

        ArrayList<Registro> listaDeRegistros = diccionario.get(dominio);

        if (listaDeRegistros == null) {
            listaDeRegistros = new ArrayList<Registro>();
            diccionario.put(dominio, listaDeRegistros);
        }

        listaDeRegistros.add(registro);

        String linea = dominio + " " + tipo + " " + valor;

        try {
            escribirFicheroRegistros(linea);
        } catch (IOException e) {
            return "500 Server error";
        }


        return "200 Record added";

    }

    private static void leerFicheroRegistros(File file, HashMap<String, ArrayList<Registro>> diccionario) {
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
            System.out.println("500 Server error - Error leyendo fichero: " + e.getMessage());
        }
    }

    private static void escribirFicheroRegistros(String linea) throws IOException {
        FileWriter fichero = new FileWriter("registros.txt", true);
        PrintWriter writer = new PrintWriter(new BufferedWriter(fichero));
        writer.append("\n").append(linea);
        writer.close();
        fichero.close();

    }





}
