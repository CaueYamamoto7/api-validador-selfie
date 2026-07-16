package br.com.assinapdf.api_validador.controller;

import br.com.assinapdf.api_validador.dto.RespostaValidacao;
import br.com.assinapdf.api_validador.service.ValidadorService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController 
@RequestMapping("/api/documentos") 
@CrossOrigin(origins = "*") 
public class ValidadorController {
    private final ValidadorService validadorService;
        public ValidadorController(ValidadorService validadorService) {
            this.validadorService = validadorService;
        }
        @PostMapping("/validar")
        public RespostaValidacao validarDocumento(@RequestParam("foto") MultipartFile arquivo) {
            return validadorService.processarDocumento(arquivo);
        }
}