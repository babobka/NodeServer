package ru.babobka.nodemasterserver.service;

import ru.babobka.nodemasterserver.model.AuthResult;
import ru.babobka.nodeserials.RSA;


import java.net.Socket;

/**
 * Created by dolgopolov.a on 29.10.15.
 */
public interface AuthService {

	AuthResult getAuthResult(RSA rsa,  Socket socket);

}
