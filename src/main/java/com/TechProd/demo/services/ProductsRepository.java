package com.TechProd.demo.services;

import org.springframework.data.jpa.repository.JpaRepository;

import com.TechProd.demo.models.Product;

public interface ProductsRepository extends JpaRepository<Product, Integer> {

}