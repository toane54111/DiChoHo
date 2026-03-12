package com.example.gomarket;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SanPhamAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<SanPham> danhSachSanPham;

    // Constructor
    public SanPhamAdapter(Context context, ArrayList<SanPham> danhSachSanPham) {
        this.context = context;
        this.danhSachSanPham = danhSachSanPham;
    }

    @Override
    public int getCount() {
        return danhSachSanPham.size();
    }

    @Override
    public Object getItem(int position) {
        return danhSachSanPham.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // 1. Tạo View từ layout
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_san_pham, parent, false);
        }

        // 2. Lấy sản phẩm hiện tại
        SanPham sanPham = danhSachSanPham.get(position);

        // 3. Ánh xạ các View
        ImageView imgSanPham = convertView.findViewById(R.id.imgSanPham);
        TextView tvTenSanPham = convertView.findViewById(R.id.tvTenSanPham);
        TextView tvGiaGoc = convertView.findViewById(R.id.tvGiaGoc);
        TextView tvGiaKhuyenMai = convertView.findViewById(R.id.tvGiaKhuyenMai);

        // 4. Gán dữ liệu
        imgSanPham.setImageResource(sanPham.getHinhAnh());
        tvTenSanPham.setText(sanPham.getTenSanPham());
        tvGiaGoc.setText(sanPham.getGiaGoc());
        tvGiaKhuyenMai.setText(sanPham.getGiaKhuyenMai());

        // 5. Gạch ngang giá gốc
        tvGiaGoc.setPaintFlags(tvGiaGoc.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        return convertView;
    }
}