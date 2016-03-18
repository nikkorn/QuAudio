package quclient.Server;

/**
 * The potential outcomes of sending a handshake to the server
 * @author Nikolas Howard
 *
 */
public enum HandshakeResponse {
    ACCEPTED,
    WRONG_ACCESS_PASSWORD,
    CONNECTION_FAILED,
    DECLINED,
    CLIENT_ALREADY_CONNECTED,
    UNIDENTIFIED
}