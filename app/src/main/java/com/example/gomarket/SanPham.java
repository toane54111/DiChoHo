package com.example.gomarket;

public class SanPham {

    private String tenSanPham;
    private String giaGoc;
    private String giaKhuyenMai;
    private String moTa;
    private int hinhAnh;
    private String imageUrl; // URL ảnh từ API (Bách Hóa Xanh)

    // Constructor cũ (fallback hardcoded)
    public SanPham(String tenSanPham, String giaGoc, String giaKhuyenMai, String moTa, int hinhAnh) {
        this(tenSanPham, giaGoc, giaKhuyenMai, moTa, hinhAnh, null);
    }

    // Constructor mới với imageUrl
    public SanPham(String tenSanPham, String giaGoc, String giaKhuyenMai, String moTa, int hinhAnh, String imageUrl) {
        this.tenSanPham = tenSanPham;
        this.giaGoc = giaGoc;
        this.giaKhuyenMai = giaKhuyenMai;
        this.moTa = moTa;
        this.hinhAnh = hinhAnh;
        this.imageUrl = imageUrl;
    }

    public String getTenSanPham() { return tenSanPham; }
    public String getGiaGoc() { return giaGoc; }
    public String getGiaKhuyenMai() { return giaKhuyenMai; }
    public String getMoTa() { return moTa; }
    public int getHinhAnh() { return hinhAnh; }
    public String getImageUrl() { return imageUrl; }
}