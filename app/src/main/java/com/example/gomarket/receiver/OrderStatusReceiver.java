package com.example.gomarket.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.gomarket.service.OrderPollingService;

public class OrderStatusReceiver extends BroadcastReceiver {

    private static final String TAG = "OrderStatusReceiver";

    public interface OnOrderStatusChangedListener {
        void onOrderStatusChanged(int orderId, String status, String statusText);
    }

    private OnOrderStatusChangedListener listener;

    public OrderStatusReceiver() {}

    public OrderStatusReceiver(OnOrderStatusChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (OrderPollingService.ACTION_ORDER_STATUS_CHANGED.equals(intent.getAction())) {
            int orderId = intent.getIntExtra(OrderPollingService.EXTRA_ORDER_ID, -1);
            String status = intent.getStringExtra(OrderPollingService.EXTRA_ORDER_STATUS);
            String statusText = intent.getStringExtra(OrderPollingService.EXTRA_ORDER_STATUS_TEXT);

            Log.d(TAG, "Order #" + orderId + " status: " + status);

            // Thông báo cho listener (Activity)
            if (listener != null) {
                listener.onOrderStatusChanged(orderId, status, statusText);
            }

            // Hiển thị Toast
            if ("COMPLETED".equals(status)) {
                Toast.makeText(context,
                        "Đơn hàng #" + orderId + " đã hoàn thành!",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context,
                        "Đơn #" + orderId + ": " + statusText,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
