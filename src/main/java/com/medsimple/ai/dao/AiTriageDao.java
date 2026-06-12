package com.medsimple.ai.dao;

import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;

public interface AiTriageDao {

    TriageResult analyze(EpisodeContext context);
}
