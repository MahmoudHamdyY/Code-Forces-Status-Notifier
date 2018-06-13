package co.android.modcru.codeforcesstatusnotifier;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

/**
 * Created by mahmoud on 24/04/18.
 */

public class ListAdapter extends ArrayAdapter<Submission>{

    private Context context;
    public ListAdapter(@NonNull Context context, int resource, List<Submission> items) {
        super(context, resource, items);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if(v == null)
        {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_item, null);
        }
        try {

            Submission submission = getItem(position);
            TextView name, time, verdict, data;
            name = v.findViewById(R.id.problemName);
            time = v.findViewById(R.id.submissionTime);
            verdict = v.findViewById(R.id.verdict);
            data = v.findViewById(R.id.data);
            String prob = submission.getContestId() + ": " + submission.getIndex() + ", " + submission.getName();
            name.setText(prob);
            long t = submission.getTime();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(t * 1000);
            final String TIME = DateFormat.format("hh:mm", cal).toString();
            final String DATE = DateFormat.format("dd-MM-yyyy", cal).toString();
            String date = DATE + " " + TIME;
            time.setText(date);
            if (submission.getVerdic().equals("OK")) {
                verdict.setText(R.string.accepted);
                verdict.setTextColor(this.context.getResources().getColor(R.color.accepted));
            } else {
                String ver = submission.getVerdic();
                ver += "On Test : ";
                ver += Integer.toString(submission.getTests() + 1);
                verdict.setText(ver);
                verdict.setTextColor(this.context.getResources().getColor(R.color.wrongAnswer));
            }
            String da = submission.getRunTime() + " ms ,";
            da += Double.toString(Double.parseDouble(submission.getMemory()) / 1024.0) + " KB";
            data.setText(da);
        }catch (IndexOutOfBoundsException ignored) {}
        return v;
    }
}
