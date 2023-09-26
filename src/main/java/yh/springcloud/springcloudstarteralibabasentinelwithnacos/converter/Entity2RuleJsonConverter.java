package yh.springcloud.springcloudstarteralibabasentinelwithnacos.converter;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.cloud.sentinel.datasource.converter.JsonConverter;
import com.alibaba.cloud.sentinel.datasource.converter.SentinelConverter;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yh.springcloud.springcloudstarteralibabasentinelwithnacos.entity.AuthorityRuleEntity;
import yh.springcloud.springcloudstarteralibabasentinelwithnacos.entity.ParamFlowRuleEntity;

import java.io.IOException;
import java.util.*;

/**
 * <p>参考JsonConverter的父类SentinelConverter进行重构，除了convertRule方法以外，剩下的内容完全照抄SentinelConverter。</p>
 * <p>主要目的是将不匹配的RuleEntity转换为Rule，目前已知不匹配的是ParamFlowRuleEntity、AuthorityRuleEntity。</p>
 * <p>sentinel-dashboard存入nacos的是序列化为json的RuleEntity，而sentinel-datasource-nacos反序列化的目标对象是Rule，
 * 即将RuleEntity根据参数反序列化为Rule，RuleEntity不匹配的原因是它们将Rule作为了成员变量，因此无法正常获取参数。</p>
 * <p>目前ParamFlowRuleEntity、AuthorityRuleEntity将Rule作为了成员变量，通过该成员变量封装策略；
 * 而其他的几个RuleEntity的成员变量直接拥有与Rule相同的成员变量，并不是通过Rule封装，因此可以反序列化为Rule</p>
 * @param <T>
 */
public class Entity2RuleJsonConverter<T> extends JsonConverter<T> {

    private static final Logger log = LoggerFactory.getLogger(SentinelConverter.class);

    private final ObjectMapper objectMapper;

    private final Class<T> ruleClass;

    public Entity2RuleJsonConverter(ObjectMapper objectMapper, Class<T> ruleClass) {
        super(objectMapper, ruleClass);
        this.objectMapper = objectMapper;
        this.ruleClass = ruleClass;
    }

    @Override
    public Collection<Object> convert(String source) {
        Collection<Object> ruleCollection;

        // hard code
        if (ruleClass == FlowRule.class || ruleClass == DegradeRule.class
                || ruleClass == SystemRule.class || ruleClass == AuthorityRule.class
                || ruleClass == ParamFlowRule.class) {
            ruleCollection = new ArrayList<>();
        }
        else {
            ruleCollection = new HashSet<>();
        }

        if (StringUtils.isEmpty(source)) {
            log.info("converter can not convert rules because source is empty");
            return ruleCollection;
        }
        try {
            List sourceArray = objectMapper.readValue(source,
                    new TypeReference<List<HashMap>>() {
                    });

            for (Object obj : sourceArray) {
                try {
                    String item = objectMapper.writeValueAsString(obj);
                    Optional.ofNullable(convertRule(item))
                            .ifPresent(ruleCollection::add);
                }
                catch (IOException e) {
                    log.error("sentinel rule convert error: " + e.getMessage(), e);
                    throw new IllegalArgumentException(
                            "sentinel rule convert error: " + e.getMessage(), e);
                }
            }
        }
        catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            else {
                throw new RuntimeException("convert error: " + e.getMessage(), e);
            }
        }
        return ruleCollection;
    }

    /**
     * 基于SentinelConverter的convertRule方法进行修改，针对ParamFlowRule、AuthorityRule的情况，
     * 先获取RuleEntity，再获取其Rule
     * @param ruleStr
     * @return
     * @throws IOException
     */
    private Object convertRule(String ruleStr) throws IOException {
        if (ruleClass == ParamFlowRule.class) {
            return objectMapper.readValue(ruleStr, ParamFlowRuleEntity.class).getRule();
        } else if (ruleClass == AuthorityRule.class) {
            return objectMapper.readValue(ruleStr, AuthorityRuleEntity.class).getRule();
        }
        return objectMapper.readValue(ruleStr, ruleClass);
    }
}
