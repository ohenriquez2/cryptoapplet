package es.uji.apps.cryptoapplet.crypto;

import es.uji.apps.cryptoapplet.config.CryptoAppletException;

@SuppressWarnings("serial")
public class ValidationException extends CryptoAppletException
{
    public ValidationException()
    {
        super();
    }
    
    public ValidationException(Throwable e)
    {
        super(e);
    }
}
