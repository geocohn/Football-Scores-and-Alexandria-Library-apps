package barqsoft.footballscores.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class ScoresWidgetListviewService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ScoresRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

