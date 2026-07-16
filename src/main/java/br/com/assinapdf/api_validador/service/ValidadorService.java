package br.com.assinapdf.api_validador.service; 

import br.com.assinapdf.api_validador.dto.RespostaValidacao;
import org.opencv.core.Core; 
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import nu.pattern.OpenCV;

import jakarta.annotation.PostConstruct; 
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class ValidadorService {

    private CascadeClassifier detectorRosto;

    @PostConstruct
    public void inicializarOpenCV() {
        try {
            OpenCV.loadShared();

            Path arquivoTemporario = Files.createTempFile("haarcascade_frontalface", ".xml");
        
            String urlModelo = "https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml";
            
            try (InputStream in = new URL(urlModelo).openStream()) {
                Files.copy(in, arquivoTemporario, StandardCopyOption.REPLACE_EXISTING);
            }

            this.detectorRosto = new CascadeClassifier(arquivoTemporario.toString());
            
            System.out.println("\n============================================================");
            System.out.println("[OPENCV] - DETECTOR DE ROSTOS PRONTO E CARREGADO COM SUCESSO!");
            System.out.println("============================================================\n");

        } catch (Exception e) {
            System.err.println("⚠️ [ERRO OPENCV] - Falha ao carregar o OpenCV ou baixar o modelo Haar Cascade.");
            System.err.println("Erro detalhado: " + e.getMessage());
        }
    }

    public RespostaValidacao analisarSelfie(MultipartFile arquivo) {
        try {
            BufferedImage imagem = ImageIO.read(arquivo.getInputStream());
            
            if (imagem == null) {
                return new RespostaValidacao(false, "O arquivo enviado não é uma imagem válida.", 0, 0, 0, "Inválido", 0, 0.0);
            }
            int largura = imagem.getWidth();
            int altura = imagem.getHeight();

            if (largura < 400 || altura < 400) {
                return new RespostaValidacao(false, "Resolução muito baixa. Tire uma foto melhor.", largura, altura, 0, "Não analisado", 0, 0.0);
            }

            double somaLuminancia = 0;
            int totalPixels = largura * altura;

            for (int x = 0; x < largura; x++) {
                for (int y = 0; y < altura; y++) {
                    int rgb = imagem.getRGB(x, y);                          
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    double luminancia = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                    somaLuminancia += luminancia;
                }
            }
            double brilhoMedio = somaLuminancia / totalPixels;
            long brilhoMedioArredondado = Math.round(brilhoMedio);
            boolean brilhoBom = true;
            String statusBrilho = "Excelente";
            if (brilhoMedio < 50) {
                brilhoBom = false;
                statusBrilho = "Muito escura (Evite locais sem iluminação)";
            } else if (brilhoMedio > 160) {
                brilhoBom = false;
                statusBrilho = "Muito clara (Evite luz direta ou flash excessivo)";
            }

            int rostosDetectados = 0;
            double scoreNitidez = 0.0;
            
            byte[] bytesDoArquivo = arquivo.getBytes();
            Mat matImagem = Imgcodecs.imdecode(new MatOfByte(bytesDoArquivo), Imgcodecs.IMREAD_UNCHANGED);
            
            if (matImagem != null && !matImagem.empty()) {
                scoreNitidez = calcularScoreNitidez(matImagem);

                if (detectorRosto != null && !detectorRosto.empty()) {
                    MatOfRect facesDetectadas = new MatOfRect();
                    detectorRosto.detectMultiScale(matImagem, facesDetectadas);
                    rostosDetectados = facesDetectadas.toArray().length;
                }
                
                matImagem.release();
            }

            boolean aprovado = true;
            String mensagem;

            double limiteNitidez = 100.0; 

            if (!brilhoBom) {
                aprovado = false;
                mensagem = "Selfie rejeitada: O ambiente está com iluminação inadequada (" + statusBrilho + ").";
            } else if (scoreNitidez < limiteNitidez) {
                aprovado = false;
                mensagem = String.format("Selfie rejeitada: A foto está tremida ou embaçada (Nitidez: %.1f). Segure o celular firme e tente novamente.", scoreNitidez);
            } else if (rostosDetectados == 0) {
                aprovado = false;
                mensagem = "Selfie rejeitada: Nenhum rosto humano foi detectado. Centralize seu rosto na câmera.";
            } else if (rostosDetectados > 1) {
                aprovado = false;
                mensagem = "Selfie rejeitada: Encontramos " + rostosDetectados + " rostos na imagem. Tire a foto sozinho.";
            } else {
                mensagem = "Selfie aprovada! Rosto detectado com ótima nitidez e iluminação.";
            }

            return new RespostaValidacao(
                aprovado, 
                mensagem, 
                largura, 
                altura, 
                brilhoMedioArredondado, 
                statusBrilho,
                rostosDetectados,
                scoreNitidez
            );

        } catch (IOException e) {
            return new RespostaValidacao(false, "Erro ao processar imagem: " + e.getMessage(), 0, 0, 0, "Erro", 0, 0.0);
        }
    }

    private double calcularScoreNitidez(Mat src) {
        Mat gray = new Mat();
        if (src.channels() > 1) {
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            src.copyTo(gray);
        }

        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stdDev);

        double variancia = Math.pow(stdDev.toArray()[0], 2);

        gray.release();
        laplacian.release();
        mean.release();
        stdDev.release();

        return variancia;
    }
}