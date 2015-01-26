import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
 
import javax.swing.*;
 
 
public class ChatClient {
 
    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica
 
    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );
     
    Socket clientSocket;
    PrintWriter print;


    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
    }
 
    public void parsingTheMessage(final String in_message){

        String message = in_message.replaceAll("(\\r|\\n)", "");
             
        String[] parsedMessage = message.split(" ");
        String messageToAppend = message;

        if(parsedMessage[0].equals("NEWNICK")){
            messageToAppend = "";
            messageToAppend = parsedMessage[1] + " mudou de nome para " + parsedMessage[2];
        }

        else if(parsedMessage[0].equals("JOINED")){
            messageToAppend = "";
            messageToAppend = "O utilizador " + parsedMessage[1] + " entrou na sala!";
        }

        else if(parsedMessage[0].equals("MESSAGE")){
            messageToAppend = "";
            messageToAppend = parsedMessage[1] + ": ";
            String tmpString = "MESSAGE " + parsedMessage[1];
            for (int i = tmpString.length(); i < message.length(); i++) {
                    messageToAppend += message.charAt(i);
            }
        }

        else if(parsedMessage[0].equals("LEFT")){
            messageToAppend = "";
            messageToAppend = "O utilizador " + parsedMessage[1] + " saiu da sala!";
        }

        else if(parsedMessage[0].equals("PRIVATE")){
            messageToAppend = "";
            messageToAppend = "PRIVATE from " + parsedMessage[1];
            String tmpString = "MESSAGE " + parsedMessage[1];
            for (int i = tmpString.length(); i < message.length(); i++) {
                messageToAppend += message.charAt(i);
            }
        }

        else if(parsedMessage[0].equals("BYE")){
            messageToAppend = "";
            messageToAppend = "Saiste do Chat! Até à próxima!";
            System.exit(1);
        }
        
        printMessage(messageToAppend+'\n');
    }
     
    // Construtor
    public ChatClient(String server, int port) throws IOException {
 
        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                   chatBox.setText("");
                }
            }
        });
        // --- Fim da inicialização da interface gráfica
 
        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
        clientSocket = new Socket(server, port);
        print = new PrintWriter(clientSocket.getOutputStream(), true);
    }
 
 
    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        // PREENCHER AQUI com código que envia a mensagem ao servidor
        print.println(message);
    }
 
     
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String writeClient; 

        while((writeClient = (String) inFromUser.readLine()) != null ){

            parsingTheMessage(writeClient + '\n');
        }
    }
     
 
    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
 
}