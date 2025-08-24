package com.example.SpringEcom.service;

import com.example.SpringEcom.model.Product;
import com.example.SpringEcom.repo.ProductRepo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ImageModel imageModel; // Directly inject the Spring AI ImageModel

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Product getProductById(int id) {
        return productRepo.findById(id).orElse(new Product(-1));
    }

    public Product addProduct(Product product, MultipartFile image) throws IOException {
        product.setImageName(image.getOriginalFilename());
        product.setImageType(image.getContentType());
        product.setImageData(image.getBytes());
        return productRepo.save(product);
    }

    public Product updateProduct(Product product, MultipartFile image) throws IOException {
        product.setImageName(image.getOriginalFilename());
        product.setImageType(image.getContentType());
        product.setImageData(image.getBytes());
        return productRepo.save(product);
    }

    public void deleteProduct(int id) {
        productRepo.deleteById(id);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepo.searchProducts(keyword);
    }

    public String generateDescription(String name, String category) {
        String descPrompt = String.format("""
                Write a concise and professional product description for an e-commerce listing.

                Product Name: %s
                Category: %s

                Keep it simple, engaging, and highlight its primary features or benefits.
                Avoid technical jargon and keep it customer-friendly.
                Limit the description to 250 characters maximum.
        """, name, category);

        return chatClient.prompt(descPrompt)
                .call()
                .content(); // Use .content(), or use your version's accessor as needed
    }

    // Inline method for AI image generation, NO dependency on a separate service
    public byte[] generateImage(String name, String category, String description) {
        String imagePrompt = String.format("""
                Generate a highly realistic, professional-grade e-commerce product image.

                Product Details:
                -Category: %s
                -Name: '%s'
                -Description: %s

                Requirements:
                - Use a clean, minimalistic, white or very light grey background.
                - Ensure the product is well-lit with soft, natural-looking lighting.
                - Add realistic shadows and soft reflections to ground the product naturally.
                - No humans, brand logos, watermarks, or text overlays should be visible.
                - Showcase the product from its most flattering angle that highlights key features.
                - Ensure the product occupies a prominent position in the frame, centered or slightly off-centered.
                - Maintain a high resolution and sharpness, ensuring all textures, colors, and details are clear.
                - Follow the typical visual style of top e-commerce websites like Amazon, Flipkart, or Shopify.
                - Make the product appear life-like and professionally photographed in a studio setup.
                - The final image should look immediately ready for use on an e-commerce website without further editing.
        """, category, name, description);

        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .N(1)
                .width(1024)
                .height(1024)
                .quality("standard")
                .responseFormat("url")
                .model("dall-e-3")
                .build();

        ImageResponse response = imageModel.call(new ImagePrompt(imagePrompt, options));
        String imageUrl = response.getResult().getOutput().getUrl();

        try {
            return new URL(imageUrl).openStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
