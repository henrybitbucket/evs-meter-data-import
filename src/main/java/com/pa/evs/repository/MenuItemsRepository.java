package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.MenuItems;

@Repository
public interface MenuItemsRepository extends JpaRepository<MenuItems, Long> {

	Optional<MenuItems> findByAppCode(String string);

}
