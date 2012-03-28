package es.uji.security.crypto.openxades.digidoc.c14n;


public abstract interface TinyXMLParser_Handler
{

    abstract public void startDocument();

    abstract public void endDocument();

    abstract public void nestedElement(TinyXMLParser_NestedElement e);

    abstract public void startElement(TinyXMLParser_Element e);

    abstract public void endElement(TinyXMLParser_Element e);

    abstract public void PI(TinyXMLParser_Tag e);

    abstract public void text(TinyXMLParser_TextNode str);

    abstract public void comment(TinyXMLParser_Comment str);

    abstract public void cdata(TinyXMLParser_CData str);

}
