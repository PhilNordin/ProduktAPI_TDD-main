package com.example.produktapi.service;


import com.example.produktapi.exception.BadRequestException;
import com.example.produktapi.exception.EntityNotFoundException;
import com.example.produktapi.model.Product;
import com.example.produktapi.repository.ProductRepository;
import jakarta.inject.Inject;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock //För att matcha mot DB??
    private ProductRepository repository;

    @InjectMocks //En underklass av orginalet
    private ProductService undertest;

    @Captor // för att fånga argument
    ArgumentCaptor<Product> productCaptor;

    @Test  //getAllProducts
    void getAllProducts() {
        //WHEN
        undertest.getAllProducts();
        //THEN
        verify(repository).findAll(); // kollar om vi kan nå metoden findALL() när repository körs. svar ja

        //verify(repository, times(2)).findAll(); för att få tested att faila
        //verify(repository).deleteAll(); //testar fel metod för att säkerställa att vi inte når delete-metoden

        verifyNoMoreInteractions(repository);
    }

    @Test //getAllCategories
    void whenGetAllCategories_thenExactlyOneInteractionWithRepositoryMethodFindAllCategories() { //notis - inte TDD
        //when
        undertest.getAllCategories();
        //then
        verify(repository, times(1)).findAllCategories(); // 2 - fail, 1 success, hämtas en gång
        verifyNoMoreInteractions(repository); //kollar att den inte anropar flera gånger
    }

    @Test
    void getProductsByCategory() {
        //given
        String existingCatergory = "men";
        Product product = new Product("hockeyklubba",300.0,"men","hej","bild");
        when(repository.findByCategory(existingCatergory)).thenReturn(List.of(product));
        //given(repository.findByCategory(existingCatergory)).willReturn(List.of(product));

        //when
        List<Product> productByCategory = undertest.getProductsByCategory(existingCatergory);

        //then
        assertEquals(1,productByCategory.size()); // kollar om produkten ligger i kaategorin
        assertEquals("hockeyklubba",productByCategory.get(0).getTitle()); // kollar så det är samma titel
        assertEquals("men", productByCategory.get(0).getCategory()); // hämtar även ut kategorinamn för att dubbelkolla


        //System.out.println(existingCatergory);
    }

    @Test //getProductById() - normalflöde
    void getProductById_givenExistingId_thenReceiveProduct() {

        //given - sätter upp ett scenario där id inte hittas genom att returnera en tom optional när vi söker efter ID't.
        Integer id = 2;
        when(repository.findById(id)).thenReturn(Optional.empty());

        //when - getProductById anropas med det angivna ID´t i assertThrows, förväntas en EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,()-> {
            undertest.getProductById(id);
        });

        //then - Kontrollerar att exception innehåller rätt meddelande, att det angivna ID't inte hittades
        assertEquals("Produkt med id " + id + " hittades inte", exception.getMessage() );

    }
    @Test //getProductById() - felflöde
    void givenNotExistingId_whenGetProductById_thenThrowEntityNotFoundException() {
        //given
        Integer id = 1;

        //Produkten behövs inte
        Product product = new Product(
                "titel",
                200.0,
                "",
                "",
                ""
        );


        //when   //returnerar tomt id om int id finns
        when(repository.findById(id)).thenReturn(Optional.empty()); // finns den ska den komma tillbaka, annars som empty

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()->{
           undertest.getProductById(id);
            });
        //then
        assertEquals("Produkt med id " +id+ " hittades inte", exception.getMessage());


    }
    @Test //addProduct() - normalflöde
    void whenAddingAProduct_thenSaveMethodShouldBeCalled(){

        //given - skapar en ny produkt
        Product product = new Product(
                "Telefon",
                5000.0,
                "",
                "",
                "");

        //when - lägger till produkten
        undertest.addProduct(product);

        //then - Kollar att repository.save() anropas en gån med rätt produkt
        verify(repository, times(1)).save(productCaptor.capture());
        assertEquals(product, productCaptor.getValue());



    }


    @Test //addProduct() - felflöde
    void givenNewProduct_whenAddingProductWithDuplicateTitle_thenThrowError() {
        //given
        String title = "test-titel";
        Product product = new Product(title,300.0,"","","");
        given(repository.findByTitle(title)).willReturn(Optional.of(product));

        //when

        //then
        BadRequestException exception = assertThrows(BadRequestException.class,
                //when
                ()-> undertest.addProduct(product));

        verify(repository, times(1)).findByTitle(title); // success
        verify(repository, times(0)).save(any()); // times() kan bytas till never()
        assertEquals("En produkt med titeln: test-titel finns redan", exception.getMessage());
    }

    @Test //update product - normalflöde
    void updateProduct_giveValidId_whenTryingToUpdateProduct_thenUpdateProductById(){
        //given
        Integer id = 1; //skapar id

        Product product = new Product(
                "titel",
                200.0,
                "category",
                "description",
                "url"
        );

        product.setId(id);

        //when
        Product updatedProduct = new Product(
                "kalle",
                450.0,
                "",
                "",
                "url"
        );

        //updatedProduct.setTitle("Updated");  // updaterar titel

        when(repository.findById(id)).thenReturn(Optional.of(updatedProduct));  // optional of - kolla orginalklassen
        when(repository.save(updatedProduct)).thenReturn(updatedProduct);

        Product result = undertest.updateProduct(updatedProduct,id);

        //then
        verify(repository).save(productCaptor.capture());
        assertEquals("kalle",result.getTitle());  //byt namn för felmeddelande
    }

    @Test  //felflöde - update product given nonValid id exception
    void updateProduct_givenNotValidId_whenTryingToUpdateProduct_thenThrowEntityNotFoundException() {
        //given
        Integer id = 1;

        Product updateProduct = new Product("",50.0,"","","");

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, ()->{
            //when
            undertest.updateProduct(updateProduct, id); // om id byts med 2 = throw exception
        });

        //then
        assertEquals(id,id);
        assertEquals("Produkt med id " +id+ " hittades inte", exception.getMessage());

    }

    @Test
    void testDeleteProduct_givenValidId_whenTryingToDeleteProduct_thenDeleteProductById() {

        //given
        Integer id = 1;

        Product product = new Product(
                "football",
                299.0,
                "balls",
                "hej",
                "bild"
        );
        product.setId(id);

        //when
        when(repository.findById(id)).thenReturn(Optional.of(product));
        undertest.deleteProduct(id);

        //then
        verify(repository, times(1)).deleteById(id);

        assertEquals(id,id);

    }

    @Test // DeleteProduct - felhantering aka felflöde felflöde är en felhanteringflöde
    void deleteProduct2_givenNotValidId_whenTryingToDelete_thenThrowEntityNotFoundException() {
        //felhantering
        //given
        Integer id = 1; //vi ger id vi skapar

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
        //when
        undertest.deleteProduct(id);  //on id byts med 2 = throws excp.
        });

        //then
        assertEquals(1,id);
        assertEquals("Produkt med id " + id + " hittades inte", exception.getMessage());
    }
}