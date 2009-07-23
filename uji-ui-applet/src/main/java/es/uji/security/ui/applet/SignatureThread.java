package es.uji.security.ui.applet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;

import es.uji.security.crypto.ISignFormatProvider;
import es.uji.security.crypto.SignatureOptions;
import es.uji.security.crypto.SignatureResult;
import es.uji.security.crypto.SupportedDataEncoding;
import es.uji.security.crypto.SupportedSignatureFormat;
import es.uji.security.crypto.openxades.OpenXAdESCoSignatureFactory;
import es.uji.security.crypto.openxades.OpenXAdESSignatureFactory;
import es.uji.security.keystore.IKeyStore;
import es.uji.security.keystore.X509CertificateHandler;
import es.uji.security.ui.applet.io.InputParams;
import es.uji.security.ui.applet.io.OutputParams;
import es.uji.security.util.Base64;
import es.uji.security.util.HexEncoder;
import es.uji.security.util.OS;
import es.uji.security.util.i18n.LabelManager;

public class SignatureThread extends Thread
{
    private MainWindow _mw = null;
    private int _end_percent = 0;
    private int _ini_percent = 0, _step = 0;
    private boolean hideWindow;
    private Method callback;
    private boolean showSignatureOk;

    public SignatureThread(String str)
    {
        super(str);
    }

    public void setPercentRange(int ini_percent, int end_percent, int step)
    {
        this._step = step;
        this._ini_percent = ini_percent;
        this._end_percent = end_percent;
    }

    public void setHideWindowOnEnd(boolean hideWindow)
    {
        this.hideWindow = hideWindow;
    }

    public void setCallbackMethod(Method m)
    {
        callback = m;
    }

    public void run()
    {

        IKeyStore iksh;
        guiInitialize();
        JLabel infoLabelField = _mw.getInformationLabelField();

        infoLabelField.setText(LabelManager.get("COMPUTING_SIGNATURE"));

        int inc = (this._end_percent - this._ini_percent) / 10;

        try
        {
            X509CertificateHandler selectedNode;
            try
            {
                selectedNode = (X509CertificateHandler) ((DefaultMutableTreeNode) _mw.jTree
                        .getLastSelectedPathComponent()).getUserObject();
            }
            catch (NullPointerException e)
            {
                throw new SignatureAppletException("ERROR_CERTIFICATE_NOT_SELECTED");
            }

            if (!selectedNode.isDigitalSignatureCertificate()
                    && !selectedNode.isNonRepudiationCertificate())
            {
                showSignatureOk = false;
                guiFinalize(false);
                throw new SignatureAppletException("ERROR_CERTIFICATE_USE");
            }
            try
            {
                selectedNode.getCertificate().checkValidity();
            }
            catch (CertificateException cex)
            {
                int selection = JOptionPane.showOptionDialog(_mw.getMainFrame(), LabelManager
                        .get("LABEL_CERTIFICATE_EXPIRED"), LabelManager
                        .get("LABEL_CERTIFICATE_EXPIRED_TITLE"), JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, new String[] { "Yes", "No" }, "No");
                if (selection == JOptionPane.NO_OPTION)
                {
                    showSignatureOk = false;
                    guiFinalize(false);
                    throw new SignatureAppletException("ERROR_CERTIFICATE_EXPIRED");
                }
            }

            iksh = selectedNode.getKeyStore();
            if (iksh != null)
            {
                try
                {
                    iksh.load(_mw.getPasswordTextField().getText().toCharArray());

                    // TODO: Research: Some problems of codification with 1.6 jvm and
                    // JPasswordField.
                    // iksh.load(_mw.getPasswordField().getPassword());
                }
                catch (Exception e)
                {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(os);
                    e.printStackTrace(ps);
                    String stk = new String(os.toByteArray()).toLowerCase();
                    if (stk.indexOf("incorrect") > -1)
                    {
                        showSignatureOk = false;
                        guiFinalize(false);
                        throw new SignatureAppletException("ERROR_INCORRECT_PWD");
                    }
                    else
                        infoLabelField.setText("Unexpected error!!!");
                    e.printStackTrace();
                }
                System.out.println("Certificate Alias: "
                        + iksh.getAliasFromCertificate(selectedNode.getCertificate()));
            }
            else
            {
                showSignatureOk = false;
                guiFinalize(false);
                throw new SignatureAppletException("ERR_GET_KEYSTORE");
            }

            _mw.getGlobalProgressBar().setValue(_ini_percent + inc);

            InputParams inputParams = (InputParams) _mw._aph.getInput();

            _mw.getGlobalProgressBar().setValue(_ini_percent + 2 * inc);

            OutputParams outputParams = (OutputParams) _mw.getAppHandler().getOutputParams();

            _mw.getGlobalProgressBar().setValue(_ini_percent + 3 * inc);

            // Creating an instance of the signature formater: CMS, XAdES, etc
            Class<?> sf = Class.forName(_mw.getAppHandler().getSignatureFormat().toString());
            ISignFormatProvider signer = (ISignFormatProvider) sf.newInstance();

            if (_mw.getAppHandler().getSignatureFormat().equals(SupportedSignatureFormat.XADES) || 
                _mw.getAppHandler().getSignatureFormat().equals(SupportedSignatureFormat.XADES_COSIGN))
            {
                String[] roles = _mw.getAppHandler().getSignerRole();
                String sr = "UNSET";
                
                if (roles != null && this._step < roles.length)
                {
                    sr = roles[this._step];
                }
                
                String fname = (_mw.getAppHandler().getXadesFileName() != null) ? _mw
                        .getAppHandler().getXadesFileName() : "UNSET";
                String fmimetype = (_mw.getAppHandler().getXadesFileMimeType() != null) ? _mw
                        .getAppHandler().getXadesFileMimeType() : "application/binary";

                if (_mw.getAppHandler().getSignatureFormat().equals(SupportedSignatureFormat.XADES))
                {
                    OpenXAdESSignatureFactory xs = (OpenXAdESSignatureFactory) signer;
                    xs.setSignerRole(sr);
                    xs.setXadesFileName(fname);
                    xs.setXadesFileMimeType(fmimetype);
                }
                else
                {
                    OpenXAdESCoSignatureFactory xs = (OpenXAdESCoSignatureFactory) signer;
                    xs.setSignerRole(sr);
                    xs.setXadesFileName(fname);
                    xs.setXadesFileMimeType(fmimetype);
                }
            }

            _mw.getGlobalProgressBar().setValue(_ini_percent + 4 * inc);
            
            if (_mw.jTree.getLastSelectedPathComponent() != null)
            {
                X509CertificateHandler xcert;
                try
                {
                    xcert = (X509CertificateHandler) ((DefaultMutableTreeNode) _mw.jTree
                            .getLastSelectedPathComponent()).getUserObject();
                }
                catch (NullPointerException e)
                {
                    showSignatureOk = false;
                    guiFinalize(false);
                    throw new SignatureAppletException("ERROR_CERTIFICATE_NOT_SELECTED");

                }
                if (xcert.isDigitalSignatureCertificate() || 
                    (xcert.isEmailProtectionCertificate() && 
                     _mw.getAppHandler().getSignatureFormat().equals(SupportedSignatureFormat.CMS)))
                {
                    ByteArrayOutputStream ot = new ByteArrayOutputStream();

                    InputStream in = inputParams.getSignData();

                    SupportedDataEncoding encoding = _mw.getAppHandler().getInputDataEncoding();

                    _mw.getGlobalProgressBar().setValue(_ini_percent + 5 * inc);
                    if (encoding.equals(SupportedDataEncoding.HEX))
                    {
                        HexEncoder h = new HexEncoder();
                        h.decode(new String(OS.inputStreamToByteArray(in)), ot);
                        in = new ByteArrayInputStream(ot.toByteArray());
                    }
                    else if (encoding.equals(SupportedDataEncoding.BASE64))
                    {
                        in = new ByteArrayInputStream(Base64.decode(OS.inputStreamToByteArray(in)));
                    }
                   

                    if (_mw.isShowSignatureEnabled() && ! _mw.getAppHandler().getIsBigFile())
                    {
                        int sel = JOptionPane.showConfirmDialog(_mw.getMainFrame(), _mw
                                .getShowDataScrollPane(OS.inputStreamToByteArray(in)), LabelManager
                                .get("LABEL_SHOW_DATA_WINDOW"), JOptionPane.OK_CANCEL_OPTION);
                        if (sel != JOptionPane.OK_OPTION)
                        {
                            _mw.getAppHandler().callJavaScriptCallbackFunction(
                                    _mw.getAppHandler().getJsSignCancel(), new String[] {});
                            showSignatureOk = false;
                            guiFinalize(true);
                            return;
                        }
                    }

   					InputStream sig = null;

                    _mw.getGlobalProgressBar().setValue(_ini_percent + 6 * inc);

                    IKeyStore kAux = xcert.getKeyStore();
                    SignatureResult signatureResult = null; 
                        
                    try
                    {
                        // Set up the data for the signature handling.
                        SignatureOptions sigOpt = new SignatureOptions();
                        sigOpt.setInputStreamToSign(in);
                        sigOpt.setCertificate(xcert.getCertificate());
                        sigOpt.setPrivateKey((PrivateKey) kAux.getKey(xcert.getAlias()));
                        sigOpt.setProvider(kAux.getProvider());
 						sigOpt.setSwapToFile(_mw.getAppHandler().getIsBigFile());
 
                        if (_mw.getAppHandler().getSignatureFormat().equals(SupportedSignatureFormat.CMS_HASH))
                        {
                           sigOpt.setHash(true);	
                        }

                        signatureResult = signer.formatSignature(sigOpt);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        System.out.println("Message: " + e.getMessage());
                        throw new SignatureAppletException("ERROR_COMPUTING_SIGNATURE");
                    }

                    if (signatureResult == null || !signatureResult.isValid())
                    {
                        String errorMessage = LabelManager.get("ERROR_COMPUTING_SIGNATURE");
                        
                        for (String msg : signatureResult.getErrors())
                        {
                            errorMessage+= (" - " + msg);
                        }
                        
                        infoLabelField.setText(errorMessage);
                        
                        showSignatureOk = false;
                        guiFinalize(false);
                        _mw.getAppHandler().getInputParams().flush();
                        
                        throw new SignatureAppletException(errorMessage, false);
                    }

                    _mw.getGlobalProgressBar().setValue(_ini_percent + 7 * inc);
                    // System.out.println("Setting input data ... ");

                    if (signatureResult != null && signatureResult.isValid())
                    {
                        try
                        {
                        	 encoding = _mw.getAppHandler().getOutputDataEncoding();
                        	 InputStream res= null;                            
                        	 if (encoding.equals(SupportedDataEncoding.HEX))
                             {
                        		 byte[] tmp = OS.inputStreamToByteArray(signatureResult.getSignatureData());
                        		 ByteArrayOutputStream bos= new ByteArrayOutputStream();
                                 HexEncoder h = new HexEncoder();
                                 h.encode(tmp, 0, tmp.length, bos);
                                 res= new ByteArrayInputStream(bos.toByteArray());
                             }
                             else if (encoding.equals(SupportedDataEncoding.BASE64))
                             {
                                 res = new ByteArrayInputStream(Base64.encode(OS.inputStreamToByteArray(signatureResult.getSignatureData())));
                             }
                             else
                             {
                                 res = signatureResult.getSignatureData();
                             }
                        	
                            outputParams.setSignData(res);
                        }
                        catch (Exception e)
                        {
                            System.out.println("Exception launch");
                            throw new SignatureAppletException("ERROR_CANNOT_SET_OUTPUT_DATA");
                        }
                    }
                    else
                    {
                        System.out.println("ERROR!!! al calcular la firma");
                    }
                }
                
                _mw.getGlobalProgressBar().setValue(_ini_percent + 8 * inc);
            }
            
            _mw.getGlobalProgressBar().setValue(_ini_percent + 10 * inc);

            guiFinalize(hideWindow);

            callback.invoke(null, null);
        }
        catch (SSLHandshakeException e)
        {
            infoLabelField.setText(LabelManager.get("ERROR_SSL") + ": " + e.getMessage());
            showSignatureOk = false;
            e.printStackTrace();
            try
            {
                guiFinalize(false);
            }
            catch (Exception e1)
            {
                infoLabelField.setText(LabelManager.get("ERROR_CANNOT_CLOSE_WINDOW"));
                e1.printStackTrace();
            }
        }
        catch (ClassCastException e)
        {
            e.printStackTrace();
            infoLabelField.setText(LabelManager.get("ERROR_CERTIFICATE_NOT_SELECTED"));
            try
            {
                showSignatureOk = false;
                guiFinalize(false);
            }
            catch (Exception e1)
            {
                infoLabelField.setText(LabelManager.get("ERROR_CANNOT_CLOSE_WINDOW"));
            }
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
            infoLabelField.setText(LabelManager.get("ERROR_COMPUTING_SIGNATURE") + ": "
                    + e.getMessage());
            try
            {
                showSignatureOk = false;
                guiFinalize(false);
            }
            catch (Exception e1)
            {
                infoLabelField.setText(LabelManager.get("ERROR_CANNOT_CLOSE_WINDOW"));
            }
        }
        catch (IOException e)
        {
            infoLabelField.setText(LabelManager.get("ERROR_INPUT_SOURCE"));
            showSignatureOk = false;
            e.printStackTrace();
            try
            {
                guiFinalize(false);
            }
            catch (Exception e1)
            {
                infoLabelField.setText(LabelManager.get("ERROR_CANNOT_CLOSE_WINDOW"));
                e1.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            infoLabelField.setText(e.getMessage());
            showSignatureOk = false;
            try
            {
                guiFinalize(false);
            }
            catch (Exception e1)
            {
                infoLabelField.setText(LabelManager.get("ERROR_CANNOT_CLOSE_WINDOW"));
                e1.printStackTrace();
            }
            _mw.getAppHandler().getInputParams().flush();
        }
    }

    private void guiInitialize()
    {

        if (_mw != null)
        {
            _mw.getInformationLabelField().setText(LabelManager.get("COMPUTING_SIGNATURE"));
            _mw.SignButton.setEnabled(false);
            _mw.jTree.setEnabled(false);

            _mw.getGlobalProgressBar().setIndeterminate(false);
            _mw.getGlobalProgressBar().setVisible(true);
            _mw.getGlobalProgressBar().setStringPainted(true);
        }
    }

    private void guiFinalize(boolean hideWindow) throws Exception
    {
        if (_mw != null)
        {
            if (showSignatureOk && hideWindow == true)
            {
                JOptionPane.showMessageDialog(_mw.getMainFrame(), LabelManager
                        .get("SIGN_PROCESS_OK"), "", JOptionPane.INFORMATION_MESSAGE);
                _mw.getAppHandler().getOutputParams().signOk();
            }
            _mw.getGlobalProgressBar().setVisible(false);
            _mw.jTree.setEnabled(true);
            _mw.SignButton.setEnabled(true);

            if (hideWindow)
                _mw.mainFrame.setVisible(false);
            else
                _mw.getShowSignatureCheckBox().setVisible(true);

        }
        this._ini_percent = 0;
        this._end_percent = 100;

        if (showSignatureOk && hideWindow == false)
        {
            _mw.getAppHandler().getOutputParams().signOk();
        }
    }

    public void setMainWindow(MainWindow mw)
    {
        _mw = mw;
    }

    public void setShowSignatureOk(boolean b)
    {
        showSignatureOk = b;
    }
}
