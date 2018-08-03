package co.android.modcru.codeforcesstatusnotifier;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    Context context;
    List<Submission> list;
    private AdapterView.OnItemClickListener onItemClickListener;
    private boolean isLoading;

    private OnLoadMoreListener onLoadMoreListener;


    public RecyclerViewAdapter(RecyclerView recyclerView,Context context,List<Submission> data,AdapterView.OnItemClickListener onItemClickListener)
    {
        this.context= context;
        this.list=data;
        this.onItemClickListener=onItemClickListener;
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && lastVisibleItem==getItemCount()-1) {
                    if (onLoadMoreListener != null) {
                        onLoadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }
        });
    }

    public void setOnLoadMoreListener(OnLoadMoreListener mOnLoadMoreListener) {
        this.onLoadMoreListener = mOnLoadMoreListener;
    }

    public void setLoaded() {
        isLoading = false;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater;
        View view = null;
        inflater = LayoutInflater.from(parent.getContext());
        view = inflater.inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ViewHolder viewHolder = (ViewHolder) holder;
            Submission submission = list.get(position);
            String prob = submission.getContestId() + ": " + submission.getIndex() + ", " + submission.getName();
            viewHolder.name.setText(prob);
            long t = submission.getTime();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(t * 1000);
            final String TIME = DateFormat.format("hh:mm", cal).toString();
            final String DATE = DateFormat.format("dd-MM-yyyy", cal).toString();
            String date = DATE + " " + TIME;
            viewHolder.time.setText(date);
            if (submission.getVerdic().equals("OK")) {
                viewHolder.verdict.setText(R.string.accepted);
                viewHolder.verdict.setTextColor(this.context.getResources().getColor(R.color.accepted));
            } else {
                String ver = submission.getVerdic();
                ver += "On Test : ";
                ver += Integer.toString(submission.getTests() + 1);
                viewHolder.verdict.setText(ver);
                viewHolder.verdict.setTextColor(this.context.getResources().getColor(R.color.wrongAnswer));
            }
            String da = submission.getRunTime() + " ms ,";
            da += Double.toString(Double.parseDouble(submission.getMemory()) / 1024.0) + " KB";
            viewHolder.data.setText(da);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setData(List<Submission> data)
    {
        this.list=data;
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView name, time, verdict, data;

        public ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.problemName);
            time = v.findViewById(R.id.submissionTime);
            verdict = v.findViewById(R.id.verdict);
            data = v.findViewById(R.id.data);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onItemClickListener.onItemClick(null, v, getAdapterPosition(), v.getId());
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }


}
