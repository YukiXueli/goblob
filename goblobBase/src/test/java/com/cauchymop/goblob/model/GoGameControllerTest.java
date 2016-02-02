package com.cauchymop.goblob.model;

import com.cauchymop.goblob.proto.PlayGameData;
import com.cauchymop.goblob.proto.PlayGameData.GoPlayer;
import com.google.common.collect.ImmutableList;

import org.junit.Test;

import dagger.Lazy;

import static com.cauchymop.goblob.proto.PlayGameData.GameData;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for {@link GoGameController}.
 */
public class GoGameControllerTest {

  private static final int TEST_HANDICAP = 0;
  private static final float TEST_KOMI = 7.5f;
  private static final GameDatas GAME_DATAS = new GameDatas(new Lazy<String>() {
    @Override
    public String get() {
      return "Pipo";
    }
  }, "Bimbo");
  private static final GoPlayer TEST_BLACK_PLAYER = GAME_DATAS.createGamePlayer("blackid", "black");
  private static final GoPlayer TEST_WHITE_PLAYER = GAME_DATAS.createGamePlayer("whiteid", "white");

  @Test
  public void testNew_gameData() {
    PlayGameData.GameConfiguration gameConfiguration = GAME_DATAS.createGameConfiguration(9, TEST_HANDICAP, TEST_KOMI, PlayGameData.GameType.LOCAL, TEST_BLACK_PLAYER, TEST_WHITE_PLAYER, true);
    GameData gameData = GAME_DATAS.createGameData(GameDatas.LOCAL_MATCH_ID, gameConfiguration, ImmutableList.of(GAME_DATAS.createMove(2, 3), GAME_DATAS.createMove(4, 5)), null);
    GoGameController controller = new GoGameController(GAME_DATAS, gameData, null);

    assertThat(controller.getGameData()).isEqualTo(gameData);
    GoGame goGame = controller.getGame();
    assertThat(goGame.getMoveHistory())
        .containsExactly(goGame.getPos(2, 3), goGame.getPos(4, 5));
  }

  @Test
  public void testGetGameData() {
    PlayGameData.GameConfiguration gameConfiguration = GAME_DATAS.createGameConfiguration(9, TEST_HANDICAP, TEST_KOMI, PlayGameData.GameType.LOCAL, TEST_BLACK_PLAYER, TEST_WHITE_PLAYER, true);
    GoGameController controller = new GoGameController(GAME_DATAS, GAME_DATAS.createGameData(GameDatas.LOCAL_MATCH_ID, gameConfiguration), null);
    controller.playMove(GAME_DATAS.createMove(0, 0));
    controller.playMove(GAME_DATAS.createMove(1, 1));
    controller.playMove(GAME_DATAS.createPassMove());
    assertThat(controller.getGameData()).isEqualTo(GameData.newBuilder()
        .setGameConfiguration(gameConfiguration)
        .addMove(GAME_DATAS.createMove(0, 0))
        .addMove(GAME_DATAS.createMove(1, 1))
        .addMove(GAME_DATAS.createPassMove())
        .setVersion(GAME_DATAS.VERSION)
        .setMatchId(GameDatas.LOCAL_MATCH_ID)
        .build());
  }

  private GoGameController createGoGameController() {
    return new GoGameController(GAME_DATAS, GAME_DATAS.createGameData(GameDatas.LOCAL_MATCH_ID, 9, TEST_HANDICAP, 0, PlayGameData.GameType.LOCAL, TEST_BLACK_PLAYER, TEST_WHITE_PLAYER, false), null);
  }

  @Test
  public void testToString() {
    GoGameController controller = createGoGameController();
    assertThat(controller.toString()).isNotNull();
  }
}
