package booking.service;

import booking.ds.ArrayBuf;
import booking.ds.Trace;
import booking.ds.TreeSnapshot;
import booking.model.Booking;

import java.time.LocalDateTime;

// Согласованный «срез» хранилища для показа: строки таблицы и верхние уровни дерева из одного момента.
// trace — след последней операции (для подсветки), может быть null.
public record StoreView(ArrayBuf<Booking> rows, TreeSnapshot<LocalDateTime, Booking> tree, Trace trace) { }
