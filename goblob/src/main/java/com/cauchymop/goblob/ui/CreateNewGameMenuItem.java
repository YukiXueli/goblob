package com.cauchymop.goblob.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * {@link MatchMenuItem} which creates a new game.
 */
public class CreateNewGameMenuItem extends MatchMenuItem {

  public static final String NEW_GAME_MATCH_ID = "new game";

  private final String label;

  public CreateNewGameMenuItem(String label) {
    super(NEW_GAME_MATCH_ID);
    this.label = label;
  }

  @Override
  public String getFirstLine(Context context) {
    return label;
  }

  @Override
  public String getSecondLine(Context context) {
    return null;
  }

  @Override
  public Drawable getIcon(Context context) {
    return null;
  }

  @Override
  public void start(GameStarter gameStarter) {
    gameStarter.startNewGame();
  }
}
