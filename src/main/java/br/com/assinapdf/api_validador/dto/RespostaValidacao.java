package br.com.assinapdf.api_validador.dto;

public class RespostaValidacao {
    private boolean aprovado;
    private String tipoDocumento; 
    private String mensagem;

public RespostaValidacao(boolean aprovado, String tipoDocumento, String mensagem) {
    this.aprovado = aprovado;
    this.tipoDocumento = tipoDocumento;
    this.mensagem = mensagem;
}
public boolean isAprovado() {
        return aprovado;
    }
    public void setAprovado(boolean aprovado) {
        this.aprovado = aprovado;
    }
    public String getTipoDocumento() {
        return tipoDocumento;
    }
    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
    public String getMensagem() {
        return mensagem;
    }
    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}

