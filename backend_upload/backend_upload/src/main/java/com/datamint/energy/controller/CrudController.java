package com.datamint.energy.controller;

import com.datamint.energy.model.Deactivatable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Generic Add / View / Edit / Delete. DELETE deactivates entities that implement Deactivatable (soft delete). */
public abstract class CrudController<T, R extends JpaRepository<T, Long>> {
    protected final R repo;
    protected CrudController(R repo) { this.repo = repo; }

    @GetMapping public List<T> all() { return repo.findAll(); }

    @GetMapping("/{id}") public T one(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    }

    @PostMapping public T create(@RequestBody T t) { setId(t, null); return repo.save(t); }

    @PutMapping("/{id}") public T update(@PathVariable Long id, @RequestBody T t) { one(id); setId(t, id); return repo.save(t); }

    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) {
        T t = one(id);
        if (t instanceof Deactivatable d) { d.deactivate(); repo.save(t); } else repo.delete(t);
    }

    private void setId(T t, Long id) {
        try { t.getClass().getField("id").set(t, id); } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
