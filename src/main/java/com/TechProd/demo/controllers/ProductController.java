package com.TechProd.demo.controllers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.TechProd.demo.models.Product;
import com.TechProd.demo.models.ProductDto;
import com.TechProd.demo.services.ProductsRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")

public class ProductController {
    // to request from the server container
    @Autowired
    private ProductsRepository repo;

    // to be accesible using the http get method
    @GetMapping({ "", "/" })
    public String showProductList(Model model) {
        List<Product> products = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("products", products);
        return "products/index";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        ProductDto productDto = new ProductDto();
        model.addAttribute("productDto", productDto);
        return "products/CreateProduct";
    }

    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute ProductDto productDto, BindingResult result) {
        if (productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto", "imageFile", "the image is required"));
        }

        if (result.hasErrors()) {
            return "products/CreateProduct";
        }

        // save the image in the server
        MultipartFile image = productDto.getImageFile(); // read the image from the form
        Date createdAt = new Date(); // the date allows us to create a unigque fileName to the image
        String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

        try {
            String uploadDir = "resources/images/"; // save the image in the folder images
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputstream = image.getInputStream()) {
                Files.copy(inputstream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }

        Product product = new Product();
        product.setName(productDto.getName());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        product.setImageFileName(storageFileName);
        product.setCategory(productDto.getCategory());
        product.setBrand(productDto.getBrand());
        product.setCreatedAt(createdAt);

        repo.save(product); // save the product using the reposotiry
        return "redirect:/products";
    }

    // method that display the page that allows the user to update the product
    @GetMapping("/edit")
    public String showEditPage(Model model, @RequestParam int id) {
        try {
            Optional<Product> productOptional = repo.findById(id);
            if (productOptional.isPresent()) {
                Product product = productOptional.get();
                model.addAttribute("product", product);

                ProductDto productDto = new ProductDto();
                productDto.setName(product.getName());
                productDto.setPrice(product.getPrice());
                productDto.setDescription(product.getDescription());
                productDto.setCategory(product.getCategory());
                productDto.setBrand(product.getBrand());

                model.addAttribute("productDto", productDto);
            } else {
                return "redirect:/products";
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            return "redirect:/products";
        }

        return "products/EditProduct";
    }

    @PostMapping("/edit")
    public String updateProduct(@Valid @ModelAttribute ProductDto productDto, @RequestParam int id, Model model,
            BindingResult result) {
        try {
            // Read the product details from the database and add it to the model
            Product product = repo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
            model.addAttribute("product", product);

            // Check if there are errors
            if (result.hasErrors()) {
                return "products/EditProduct";
            }

            // Check if there is a new image or not (replacing the current image)
            if (!productDto.getImageFile().isEmpty()) {
                // Delete the old image
                String uploadDir = "resources/images/";
                Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());

                try {
                    Files.delete(oldImagePath); // To delete the image
                } catch (Exception ex) {
                    System.out.println("Exception deleting old image: " + ex.getMessage());
                }

                // Save new image file
                MultipartFile image = productDto.getImageFile(); // Read the image from the form
                Date createdAt = new Date(); // the date allows us to create a unigque fileName to the image
                String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

                try (InputStream inputStream = image.getInputStream()) {
                    Files.copy(inputStream, Paths.get(uploadDir + storageFileName),
                            StandardCopyOption.REPLACE_EXISTING);
                }

                product.setImageFileName(storageFileName);
            }

            // Update the product
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());

            repo.save(product);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }

        return "redirect:/products";
    }

    @GetMapping("/delete")
    public String deleteProduct(@RequestParam int id) {
        try {
            Product product = repo.findById(id).get();

            // delete product image
            Path imagePath = Paths.get("resources/images/" + product.getImageFileName());
            try {
                Files.delete(imagePath);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }

            // delete the product from the database
            repo.delete(product);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return "redirect:/products";
    }

}