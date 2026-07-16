package br.com.assinapdf.api_validador.service;

import br.com.assinapdf.api_validador.dto.RespostaValidacao;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.regex.Pattern;

@Service 
public class ValidadorService {

    @Value("${tesseract.datapath:./tessdata}")
    private String tessdataPath;

    public RespostaValidacao processarDocumento(MultipartFile arquivoImagem) {
        try {
            InputStream is = arquivoImagem.getInputStream();
            BufferedImage imagemOriginal = ImageIO.read(is);
            
            if (imagemOriginal == null) {
                return new RespostaValidacao(false, "NENHUM", "O arquivo enviado não é uma imagem válida.");
            }

            // 1. TRATAMENTO DA IMAGEM: Converte para Preto e Branco (Grayscale)
            // Isso apaga o fundo colorido de segurança dos documentos e realça as letras escuras
            BufferedImage imagemProcessada = new BufferedImage(
                imagemOriginal.getWidth(), 
                imagemOriginal.getHeight(), 
                BufferedImage.TYPE_BYTE_GRAY
            );
            
            Graphics g = imagemProcessada.getGraphics();    
            g.drawImage(imagemOriginal, 0, 0, null);
            g.dispose();

            // 2. CONFIGURAÇÃO E EXECUÇÃO DO TESSERACT OCR
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("por");
            
            // Executamos o motor de leitura em cima da imagem tratada
            String textoExtraido = tesseract.doOCR(imagemProcessada);

            // 3. ANÁLISE E VALIDAÇÃO DE SEGURANÇA
            return validarTextoDocumento(textoExtraido);

        } catch (Exception e) {
            return new RespostaValidacao(false, "ERRO_INTERNO", "Falha ao processar a imagem: " + e.getMessage());
        }
    }

    private RespostaValidacao validarTextoDocumento(String texto) {
        // Normaliza para letras minúsculas para evitar problemas de maiúsculo/minúsculo
        String textoMinusculo = texto.toLowerCase();

        // Regex de busca: Procura se existe qualquer Data válida (padrão DD/MM/AAAA) no documento
        boolean temDataValida = Pattern.compile("\\d{2}/\\d{2}/\\d{4}").matcher(textoMinusculo).find();
        
        // Regex de busca: Procura se existe qualquer padrão de CPF válido (ex: 000.000.000-00 ou só os números)
        boolean temCPF = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}").matcher(textoMinusculo).find();

        // REGRAS PARA RG:
        // Precisa conter termos de identificação civil (e mapeamos os ruídos que o OCR gerou antes para tolerância)
        // E também obrigatoriamente precisa encontrar pelo menos uma Data (Nascimento ou Expedição)
        boolean temTermosRG = textoMinusculo.contains("república") 
                           || textoMinusculo.contains("republica")
                           || textoMinusculo.contains("registro geral")
                           || textoMinusculo.contains("identidade")
                           || textoMinusculo.contains("polegar")
                           || textoMinusculo.contains("nascimento")
                           || textoMinusculo.contains("datanascmento");

        boolean ehRG = temTermosRG && temDataValida;

        // REGRAS PARA CNH:
        // Precisa conter termos de trânsito 
        // E obrigatoriamente possuir um CPF válido E uma Data válida (Nascimento ou Validade)
        boolean temTermosCNH = textoMinusculo.contains("carteira nacional") 
                            || textoMinusculo.contains("habilitação") 
                            || textoMinusculo.contains("habilitacao") 
                            || textoMinusculo.contains("detran")
                            || textoMinusculo.contains("condutor");

        boolean ehCNH = temTermosCNH && temCPF && temDataValida;

        // RETORNO DA VALIDAÇÃO INTELIGENTE
        if (ehCNH) {
            return new RespostaValidacao(true, "CNH", "CNH identificada e validada com sucesso.");
        } else if (ehRG) {
            return new RespostaValidacao(true, "RG", "RG identificado e validado com sucesso.");
        }

        return new RespostaValidacao(false, "DESCONHECIDO", "Documento recusado. Não identificamos dados estruturais de RG ou CNH legítimos.");
    }
}