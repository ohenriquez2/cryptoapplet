package es.uji.security.crypto;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;

public class SignatureOptions
{
	private boolean hash = false;
    private boolean localFile = false;
    private boolean swapToFile = false;
    private boolean coSignEnabled = true;
    private X509Certificate certificate = null;
    private PrivateKey privateKey = null;
    private Provider provider = null;
    private InputStream dataToSign = null;

    public SignatureOptions()
    {
    }

    public boolean isHash()
    {
        return hash;
    }

    public void setHash(boolean hash)
    {
        this.hash = hash;
    }

    public boolean isLocalFile()
    {
        return localFile;
    }

    public void setLocalFile(boolean localFile)
    {
        this.localFile = localFile;
    }

    public void setSwapToFile(boolean swapToFile)
    {
        this.swapToFile = swapToFile;
    }
    
    public boolean getSwapToFile()
    {
        return this.swapToFile;
    }

    public X509Certificate getCertificate()
    {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate)
    {
        this.certificate = certificate;
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey)
    {
        this.privateKey = privateKey;
    }

    public Provider getProvider()
    {
        return provider;
    }

    public void setProvider(Provider provider)
    {
        this.provider = provider;
    }

    public InputStream getDataToSign()
    {
        return this.dataToSign;
    }

    public void setDataToSign(InputStream dataToSign)
    {
        this.dataToSign = dataToSign;
    }

    public boolean isCoSignEnabled()
    {
        return coSignEnabled;
    }

    public void setCoSignEnabled(boolean coSignEnabled)
    {
        this.coSignEnabled = coSignEnabled;
    }
}
