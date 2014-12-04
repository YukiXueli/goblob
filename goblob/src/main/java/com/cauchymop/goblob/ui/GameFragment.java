package com.cauchymop.goblob.ui;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.cauchymop.goblob.BuildConfig;
import com.cauchymop.goblob.R;
import com.cauchymop.goblob.model.GameDatas;
import com.cauchymop.goblob.model.GoGameController;
import com.cauchymop.goblob.model.GoPlayer;
import com.cauchymop.goblob.model.MonteCarlo;
import com.cauchymop.goblob.model.StoneColor;
import com.cauchymop.goblob.proto.PlayGameData;

/**
 * Game Page Fragment.
 */
public class GameFragment extends GoBlobBaseFragment implements GoBoardView.Listener {

  private static final String TAG = GoBlobBaseFragment.class.getName();
  private static final String EXTRA_GO_GAME = "GO_GAME";

  private GoGameController goGameController;
  private GoBoardView goBoardView;

  public static GameFragment newInstance(GoGameController gameController) {
    GameFragment fragment = new GameFragment();
    Bundle args = new Bundle();
    args.putSerializable(EXTRA_GO_GAME, gameController);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    Log.d(TAG, "onCreate: " + getArguments());
    if (getArguments() != null && getArguments().containsKey(EXTRA_GO_GAME) && this.goGameController == null) {
      this.goGameController = (GoGameController) getArguments().getSerializable(EXTRA_GO_GAME);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_game, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    Log.d(TAG, "onDestroyView");
    cleanBoardView();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    if (BuildConfig.DEBUG) {
      menu.add(Menu.NONE, R.id.menu_imfeelinglucky, Menu.NONE, R.string.imfeelinglucky);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_imfeelinglucky) {
      int bestMove = MonteCarlo.getBestMove(goGameController.getGame(), 1000);
      int boardSize = goGameController.getGameConfiguration().getBoardSize();
      int x = bestMove % boardSize;
      int y = bestMove / boardSize;
      goGameController.playMove(GameDatas.createMove(x, y));
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onSignInSucceeded() {
    super.onSignInSucceeded();
  }

  @Override
  public void onSignInFailed() {
    super.onSignInFailed();
  }

  @Override
  public void onSignOut() {
    super.onSignOut();
  }

  private void initViews() {
    FrameLayout boardViewContainer = (FrameLayout) getView().findViewById(R.id.boardViewContainer);
    goBoardView = new GoBoardView(getActivity().getApplicationContext(), goGameController);
    goBoardView.addListener(this);
    configureActionButton();
    boardViewContainer.addView(goBoardView);
    initFromGameState();
    enableInteractions(goGameController.isLocalTurn());
  }

  private void enableInteractions(boolean enabled) {
    Button actionButton = (Button) getView().findViewById(R.id.action_button);
    actionButton.setEnabled(enabled);
    goBoardView.setClickable(enabled);
  }

  private void configureActionButton() {
    switch(goGameController.getMode()) {
      case START_GAME_NEGOTIATION:
        break;
      case IN_GAME:
        configureActionButton(R.string.button_pass_label, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            play(GameDatas.createPassMove());
          }
        });
        break;
      case END_GAME_NEGOTIATION:
        configureActionButton(R.string.button_done_label, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            goGameController.markingTurnDone();
            endTurn();
          }
        });
        break;
    }
  }

  private void configureActionButton(int buttonLabel, View.OnClickListener clickListener) {
    Button actionButton = (Button) getView().findViewById(R.id.action_button);
    actionButton.setText(buttonLabel);
    actionButton.setOnClickListener(clickListener);
  }

  private void endTurn() {
    updateAchievements();
    sendRemoteMessages();
    getGoBlobActivity().loadGame(goGameController);
  }

  private void sendRemoteMessages() {
    switch (goGameController.getMode()) {
      case START_GAME_NEGOTIATION:
        break;
      case IN_GAME:
        if (goGameController.getCurrentPlayer().getType() == GoPlayer.PlayerType.REMOTE) {
          getGoBlobActivity().giveTurn(goGameController);
        }
        break;
      case END_GAME_NEGOTIATION:
        if (goGameController.getOpponent().getType() == GoPlayer.PlayerType.REMOTE) {
          getGoBlobActivity().keepTurn(goGameController);
        }
        if (goGameController.getCurrentPlayer().getType() == GoPlayer.PlayerType.REMOTE) {
          if (goGameController.isGameFinished()) {
            getGoBlobActivity().finishTurn(goGameController);
          } else {
            getGoBlobActivity().giveTurn(goGameController);
          }
        }
        break;
    }
  }

  @Override
  public void played(int x, int y) {
    play(GameDatas.createMove(x, y));
  }

  private void play(PlayGameData.Move move) {
    boolean played = playMoveOrToggleDeadStone(move);
    if(played) {
      endTurn();
    } else {
      buzz();
    }
  }

  private boolean playMoveOrToggleDeadStone(PlayGameData.Move move) {
    switch(goGameController.getMode()) {
      case IN_GAME:
        return goGameController.playMove(move);
      case END_GAME_NEGOTIATION:
        return goGameController.toggleDeadStone(move);
      default:
        throw new RuntimeException("Invalid mode");
    }
  }

  private void cleanBoardView() {
    if (goBoardView != null) {
      goBoardView.removeListener(this);
    }
  }

  private void initFromGameState() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        initTitleArea();
        initMessageArea();
      }
    });
  }

  private void initTitleArea() {
    TextView titleView = (TextView) getView().findViewById(R.id.title);
    ImageView titleImage = (ImageView) getView().findViewById(R.id.titleImage);
    final GoPlayer currentPlayer = goGameController.getCurrentPlayer();
    titleView.setText(currentPlayer.getName());
    titleImage.setImageResource(goGameController.getCurrentColor() == StoneColor.White ? R.drawable.white_stone : R.drawable.black_stone);
    ImageView avatarImage = (ImageView) getView().findViewById(R.id.avatarImage);
    getGoBlobActivity().getAvatarManager().loadImage(avatarImage, currentPlayer.getName());
  }

  /**
   * Display a message if needed (other player has passed...), clean the message area otherwise.
   */
  private void initMessageArea() {
    final String message;
    if (goGameController.isGameFinished()) {
      PlayGameData.Score score = goGameController.getScore();
      message = getString(R.string.end_of_game_message, score.getWinner(), score.getWonBy());
    } else if (goGameController.getMode() == GoGameController.Mode.END_GAME_NEGOTIATION) {
      message = getString(R.string.marking_message);
    } else if (goGameController.getGame().isLastMovePass()) {
      message = getString(R.string.opponent_passed_message, goGameController.getOpponent().getName());
    } else {
      message = null;
    }

    TextView messageView = (TextView) getView().findViewById(R.id.message_textview);
    messageView.setText(message);
  }

  private void updateAchievements() {
    getGoBlobActivity().unlockAchievement(getString(R.string.achievements_gamers));
    switch (goGameController.getGame().getBoardSize()) {
      case 9:
        getGoBlobActivity().unlockAchievement(getString(R.string.achievements_9x9));
        break;
      case 13:
        getGoBlobActivity().unlockAchievement(getString(R.string.achievements_13x13));
        break;
      case 19:
        getGoBlobActivity().unlockAchievement(getString(R.string.achievements_19x19));
        break;
    }
    switch (goGameController.getOpponent().getType()) {
      case LOCAL:
        getGoBlobActivity().unlockAchievement(getString(R.string.achievements_human));
        break;
    }
  }

  private void buzz() {
    try {
      Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      Ringtone r = RingtoneManager.getRingtone(getActivity().getApplicationContext(), notification);
      r.play();
    } catch (Exception e) {
      System.err.println("Exception while buzzing");
      e.printStackTrace();
    }
  }
}
