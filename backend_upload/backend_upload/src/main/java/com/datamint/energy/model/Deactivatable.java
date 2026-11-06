package com.datamint.energy.model;
/** Entities that are soft-deleted (status change) so history is never lost. */
public interface Deactivatable { void deactivate(); }
