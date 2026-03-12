package com.example.gomarket;

public class SanPham {

    private String tenSanPham;
    private String giaGoc;
    private String giaKhuyenMai;
    private String moTa;          // THÊM MỚI
    private int hinhAnh;

    // Constructor - thêm moTa
    public SanPham(String tenSanPham, String giaGoc, String giaKhuyenMai, String moTa, int hinhAnh) {
        this.tenSanPham = tenSanPham;
        this.giaGoc = giaGoc;
        this.giaKhuyenMai = giaKhuyenMai;
        this.moTa = moTa;
        this.hinhAnh = hinhAnh;
    }

    // Getter
    public String getTenSanPham() {
        return tenSanPham;
    }

    public String getGiaGoc() {
        return giaGoc;
    }

    public String getGiaKhuyenMai() {
        return giaKhuyenMai;
    }

    public String getMoTa() {
        return moTa;
    }

    public int getHinhAnh() {
        return hinhAnh;
    }
}