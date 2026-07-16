package br.com.assinapdf.api_validador.dto;

public class RespostaValidacao {
    private boolean aprovado;
    private String mensagem;
    private int largura;
    private int altura;
    private long brilhoMedio;
    private String statusBrilho;
    private int rostosDetectados;
    private double nitidez;

    public RespostaValidacao() {}

    public RespostaValidacao(boolean aprovado, String mensagem, int largura, int altura, long brilhoMedio, String statusBrilho, int rostosDetectados, double nitidez) {
        this.aprovado = aprovado;
        this.mensagem = mensagem;
        this.largura = largura;
        this.altura = altura;
        this.brilhoMedio = brilhoMedio;
        this.statusBrilho = statusBrilho;
        this.rostosDetectados = rostosDetectados;
        this.nitidez = nitidez;
    }

    public boolean isAprovado() { return aprovado; }
    public void setAprovado(boolean aprovado) { this.aprovado = aprovado; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public int getLargura() { return largura; }
    public void setLargura(int largura) { this.largura = largura; }

    public int getAltura() { return altura; }
    public void setAltura(int altura) { this.altura = altura; }

    public long getBrilhoMedio() { return brilhoMedio; }
    public void setBrilhoMedio(long brilhoMedio) { this.brilhoMedio = brilhoMedio; }

    public String getStatusBrilho() { return statusBrilho; }
    public void setStatusBrilho(String statusBrilho) { this.statusBrilho = statusBrilho; }

    public int getRostosDetectados() { return rostosDetectados; }
    public void setRostosDetectados(int rostosDetectados) { this.rostosDetectados = rostosDetectados; }

    public double getNitidez() { return nitidez; }
    public void setNitidez(double nitidez) { this.nitidez = nitidez; }
}