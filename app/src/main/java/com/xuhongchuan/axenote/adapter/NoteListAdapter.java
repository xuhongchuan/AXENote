package com.xuhongchuan.axenote.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.xuhongchuan.axenote.R;
import com.xuhongchuan.axenote.dao.NoteDao;
import com.xuhongchuan.axenote.infr.ItemTouchHelperViewHolder;
import com.xuhongchuan.axenote.data.Note;
import com.xuhongchuan.axenote.infr.ItemTouchHelperAdapter;
import com.xuhongchuan.axenote.ui.activity.EditorActivity;
import com.xuhongchuan.axenote.utils.GlobalDataCache;
import com.xuhongchuan.axenote.utils.PinyinUtil;
import com.xuhongchuan.axenote.utils.GlobalConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 便签列表适配器
 * Created by xuhongchuan on 15/10/17.
 */
public class NoteListAdapter extends RecyclerView.Adapter<NoteListAdapter.NoteListViewHolder>
        implements ItemTouchHelperAdapter {
    private static int FROM; // swap的from
    private static int TO; // swap的to
    private static boolean ENABLE = true; // 是否执行swap

    private final LayoutInflater mLayoutInflater;
    private final Context mContext;

    public NoteListAdapter(Context context) {
        this.mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public NoteListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_list_item,
                parent, false);
        NoteListViewHolder holder = new NoteListViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(NoteListViewHolder holder, int position) {
        Note note = GlobalDataCache.getInstance().getNotes().get(position);
        String title = note.getTitle();
        boolean hasImage = note.getHasImage();
        holder.titleView.setText(title);
        holder.imageIconView.setVisibility(hasImage ? View.VISIBLE : View.INVISIBLE);
        Resources res = mContext.getResources();
        holder.itemView.setBackground(res.getDrawable(R.drawable.shape_note_item_bg));

    }

    @Override
    public int getItemCount() {
        return GlobalDataCache.getInstance().getNotes().size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        notifyItemRemoved(position);
        GlobalDataCache cache = GlobalDataCache.getInstance();
        cache.deleteNote(cache.getNotes().get(position).getId());
        notifyItemRangeChanged(position, getItemCount());
        ENABLE = false; // 在onItemClear()不执行swap
    }

    /**
     * 查询
     *
     * @param query
     */
    public void filter(String query) {
        // 重新初始化数据
        GlobalDataCache cache = GlobalDataCache.getInstance();
        cache.syncNotes();
        List<Note> notes = cache.getNotes();

        PinyinUtil contrastPinyin = new PinyinUtil(); // 拼音模糊查询工具类
        List<Note> data = new ArrayList<>();

        for (Note note : notes) {
            int index = 0;
            String content = note.getContent();
            if (contrastPinyin.contains(query)) {
                index = content.indexOf(query);
            } else {
                String pinyin = contrastPinyin.getSpells(content);
                index = pinyin.indexOf(query);
            }
            if (index != -1) {
                data.add(note);
            }
        }
        notes.clear();
        notes.addAll(data);

        notifyDataSetChanged(); // 刷新RecyclerView
    }

    public class NoteListViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        TextView titleView;
        ImageView imageIconView;

        public NoteListViewHolder(final View view) {
            super(view);
            titleView = view.findViewById(R.id.title);
            imageIconView = view.findViewById(R.id.image_icon);
            view.setOnClickListener(v -> {
                // 进入便签编辑Activity，并且传递当前便签内容和索引
                Intent intent = new Intent(view.getContext(), EditorActivity.class);
                intent.putExtra("position", getPosition());
                view.getContext().startActivity(intent);
            });
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
            FROM = getPosition();
        }

        @Override
        public void onItemClear() {
            TO = getPosition();
            if (FROM != TO && ENABLE) { // 排除滑动删除
                NoteDao dao = NoteDao.getInstance();
                GlobalDataCache cache = GlobalDataCache.getInstance();
                // 交换数据库两条便签的排序值
                cache.swapNote(FROM, TO); // 交换缓存Note列表
                dao.swapPosition(FROM, TO); // 交换数据库Note列表
            }
            ENABLE = true;
            itemView.setBackground(mContext.getResources().getDrawable(R.drawable.shape_note_item_bg));
        }
    }

}
