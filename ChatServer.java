import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();
  //static private final CharsetEncoder encoder = charset.newEncoder();
  
  public static final Integer INIT = 1;
  public static final Integer OUTSIDE = 2;
  public static final Integer INSIDE = 3;
  public static final String OK = "OK"+'\n';
  public static final String ERROR = "ERROR"+'\n';

  public static Hashtable<SocketChannel, String> usersName = new Hashtable<SocketChannel, String>();
  public static Hashtable<SocketChannel, Integer> StateUsers = new Hashtable<SocketChannel, Integer>();
  public static Hashtable<SocketChannel, String> SocketChannelUsers = new Hashtable<SocketChannel, String>();
  
  static private String cmd = null ;

  static public void main( String args[] ) throws Exception {
    
	  
	//HashMap<String, SocketChannel> map = new HashMap<String, SocketChannel>();
	  
	// Parse port from command line
    int port = Integer.parseInt( args[0] );
    
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) ==
            SelectionKey.OP_ACCEPT) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );

            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ );
            
            StateUsers.put(sc, INIT);

          } else if ((key.readyOps() & SelectionKey.OP_READ) ==
            SelectionKey.OP_READ) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput( sc);

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }


  // Just read the message from the socket and send it to stdout
  static private boolean processInput( SocketChannel sc) throws IOException {
    
        		
	// Read the message to the buffer
    buffer.clear();
    sc.read( buffer );
    buffer.flip();
    
    Socket s = sc.socket();

    // If no data, close the connection
    if (buffer.limit()==0) {
      return false;
    }

    // Decode and print the message to stdout
    //String message = decoder.decode(buffer).toString();
    //System.out.print( message );
    // sc.write(encoder.encode(CharBuffer.wrap(message)));
    
	String text = decoder.decode(buffer).toString();
	
	if (text.charAt(text.length()-1) == '\n') {
		if(cmd != null){
			text = cmd + text;
			cmd = null;
		}
		
		text = text.replaceAll("(\\r|\\n)", "");
		if(text.equals(""))
			return true;
		
		String[] message_parsed = text.split(" ");
		
		if(message_parsed.length == 0) {
			return true;
		}
		
		//IMPLEMENTACAO CRIACAO E MUDANÇA NICK
		if (message_parsed[0].equalsIgnoreCase("/nick") && message_parsed.length == 2) {
			//se o nick já existe retorna erro
			if (usersName.containsValue(message_parsed[1])){
				System.out.println("Nick is already taken");
				sendMessage(sc, ERROR);
			}
			//nick nao existe, mudar nick e adicionar a hashtable
			else{
				String ChangedNick = "NEWNICK " + usersName.get(sc) + " " + message_parsed[1] + '\n';//
				Enumeration scKeys = SocketChannelUsers.keys();
				//mudanca de nick em todos os channels
				while(scKeys.hasMoreElements()){
					Object next = scKeys.nextElement();
					
					SocketChannel scTemp = (SocketChannel) next;
					
					if(SocketChannelUsers.get(next).equals(SocketChannelUsers.get(sc)) && sc != scTemp){
						sendMessage(scTemp, ChangedNick);
					}
				}
				
				if(StateUsers.get(sc) != INSIDE){
					StateUsers.put(sc, OUTSIDE);
				}
				if(StateUsers.get(sc) != INIT)
					System.out.println("NEWNICK: "  + usersName.get(sc) + " " + message_parsed[1]);
				
				usersName.put(sc, message_parsed[1]);

				sendMessage(sc, OK);
			}
			
			return true;
		}
		
		//IMPLEMENTACAO JOIN ROOM
		if(message_parsed[0].equalsIgnoreCase("/join") && message_parsed.length == 2){
			
			//utilizador acabado de se conectar, necessario nome antes de entrar numa sala de chat
			if(StateUsers.get(sc).equals(INIT)){
				sendMessage(sc, ERROR);
				return true;
			}
			
			if(StateUsers.get(sc).equals(INSIDE)){
				leaveRoom(sc, SocketChannelUsers.get(sc));
			}
			
			SocketChannelUsers.put(sc, message_parsed[1]);
			StateUsers.put(sc, INSIDE);
			sendMessage(sc, OK);
			
			joinRoom(sc, message_parsed[1]);
			
			return true;
		}
		
		if (message_parsed[0].equalsIgnoreCase("/leave") && message_parsed.length == 1) {
			if(StateUsers.get(sc).equals(INIT) && StateUsers.get(sc).equals(OUTSIDE)){
				sendMessage(sc, ERROR);
				System.out.println("User need to be in channel for leaving");
				
			}
			else{
				leaveRoom(sc, SocketChannelUsers.get(sc));
				StateUsers.put(sc, OUTSIDE);
				
			}
			
			return true;
		}
		
		//IMPLEMENTACAO LEAVE
		if (message_parsed[0].equalsIgnoreCase("/join") && message_parsed.length == 2) {
			if(StateUsers.get(sc).equals(INIT) || StateUsers.get(sc).equals(OUTSIDE)){
				sendMessage(sc, ERROR);
			}
			
			if(StateUsers.get(sc).equals(INSIDE)){
				leaveRoom(sc, SocketChannelUsers.get(sc));
				StateUsers.put(sc, OUTSIDE);
				SocketChannelUsers.remove(sc);
				sendMessage(sc, OK);
			}
			
			return true;
		}
		
		//IMPLEMENTACAO PRIV
		if (message_parsed[0].equalsIgnoreCase("/priv")) {
			Enumeration keys = usersName.keys();
			
			SocketChannel sctemp = null;
			String nickPriv = null;
			
			while(keys.hasMoreElements()){
				Object key = keys.nextElement();
				if (usersName.get(key).equals(message_parsed[1])) {
					sctemp = (SocketChannel) key;
					nickPriv = usersName.get(key);
					break;
				}
			}
			
			if(nickPriv == null){
				//nao existe utilizador com aquele nick
				sendMessage(sc, ERROR);
				return true;
			}
			
			String message = "PRIVATE " + usersName.get(sc);
			for (int i = 2; i < message_parsed.length; i++) {
				message = message + " " + message_parsed[i];
			}
			
			message = message + '\n';
			sendMessage(sctemp, message);
			sendMessage(sc, message);
			return true;
		}
		
		//IMPLEMENTACAO BYE
		if (message_parsed[0].equalsIgnoreCase("/bye")) {
			
			//se estiver em sala
			if (StateUsers.get(sc).equals(INSIDE)) {
				leaveRoom(sc, SocketChannelUsers.get(sc));
			}
			StateUsers.remove(sc);
			SocketChannelUsers.remove(sc);
			usersName.remove(sc);
			String bye = "BYE"+'\n';
			sendMessage(sc, bye);
			sc.close();
			return true;
		}
		
		
		//IMPLEMENTACAO ENVIO DE MENSAGENS
		if(StateUsers.get(sc) == INSIDE){
			String userMessage = "MESSAGE " + usersName.get(sc) + " " + text + '\n';

			if(message_parsed[0].length()>1){
				if(message_parsed[0].charAt(1)=='/'){
					System.out.println("Start the string with //");

					String tempString = "";
					for (int i = 1; i < text.length(); i++) {
						tempString = tempString + text.charAt(i);
					}
					userMessage = "MESSAGE " + usersName.get(sc) + " " + tempString + '\n';
				}
			}
			
			System.out.println("MESSAGE " + usersName.get(sc) + " " + text);

			
			Enumeration scKeys = SocketChannelUsers.keys();
			while(scKeys.hasMoreElements()){
				Object key = scKeys.nextElement();
				if(SocketChannelUsers.get(sc).equals(SocketChannelUsers.get(key))){
					SocketChannel scTemp = (SocketChannel) key;
					sendMessage(scTemp, userMessage);
				}
			}
			
			return true;
		}
		
		//utilizador esta com estado INIT
		else{
			sendMessage(sc, ERROR);
		}

	}
    
    return true;
  }
  
  
  /**
  * Percorre HashTable, envia mensagem a todos os utilizadores da nova sala, que o utilizador X saiu 
  * @param sc
  * @param room
  */
  private static void leaveRoom(SocketChannel sc, String room) {
		// TODO Auto-generated method stub
		Enumeration keys = SocketChannelUsers.keys();
		String userLeave = "LEFT " + usersName.get(sc) + '\n';
		SocketChannel scTemp;
		
		while (keys.hasMoreElements()) {
			Object actualRoom = keys.nextElement();
			scTemp = (SocketChannel) actualRoom;
			if(SocketChannelUsers.get(actualRoom).equals(room) && scTemp != sc){
				try {
					sendMessage(scTemp, userLeave);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
  
  /**
   * Percorre HashTable, envia mensagem a todos os utilizadores da nova sala, que o utilizador X entrou 
   * @param sc
   * @param room
   */
  private static void joinRoom(SocketChannel sc, String room) {
	// TODO Auto-generated method stub
	
	 Enumeration keys = SocketChannelUsers.keys();
	 SocketChannel scTemp;
	 
	 String joinUser = "JOINED " + usersName.get(sc) + '\n';
	 while(keys.hasMoreElements()){
		 Object actualRoom = keys.nextElement();
		 scTemp = (SocketChannel) actualRoom;
		 if(SocketChannelUsers.get(actualRoom).equals(room) && scTemp != sc){
			 try {
				sendMessage(scTemp, joinUser);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
	 }
}

/**
 * Envia a String message para a Socket sc.
 * @param sc
 * @param message
 * @throws IOException
 */
public static void sendMessage(SocketChannel sc, String message) throws IOException {
		
		buffer.clear();
		buffer.put(message.getBytes());
		buffer.flip();
		
		while(buffer.hasRemaining()) {
		    sc.write(buffer);
		}
	}
}