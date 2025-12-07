import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class HiloCliente implements Runnable {
    private Socket socket;
    private HashMap<String, ArrayList<Registro>> diccionario;


    public HiloCliente(Socket socket, HashMap<String, ArrayList<Registro>> diccionario) {
        this.socket = socket;
        this.diccionario = diccionario;
    }




    @Override
    public void run() {

        BufferedReader reader = null;
        PrintWriter salida = null;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            String mensaje;

            while ((mensaje = reader.readLine()) != null) {

                if (mensaje.equalsIgnoreCase("EXIT")) {
                    salida.println("200 - Hasta la proxima!");
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
            }

        } catch (Exception e) {
            if (salida != null) {
                salida.println("500 Server error" + e.getMessage());
            }
        } finally {
            ServidorDNS.quitarCliente();
            try { socket.close(); } catch (Exception ignored) {}
        }
    }



    public String resolverDominio(String[] comandos, HashMap<String, ArrayList<Registro>> diccionario) {

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

        if (resultado.isEmpty()) {
            return "404 NOT FOUND";
        }

        return resultado.toString().trim();

    }

    public String listarRegistros(String[] comandos, HashMap<String, ArrayList<Registro>> diccionario) {

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

    public String agregarRegistro(String[] comandos, HashMap<String, ArrayList<Registro>> diccionario) {

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

    private synchronized void escribirFicheroRegistros(String linea) throws IOException {
        FileWriter fichero = new FileWriter("registros.txt", true);
        PrintWriter writer = new PrintWriter(new BufferedWriter(fichero));
        writer.append("\n").append(linea);
        writer.close();
        fichero.close();
    }

}
