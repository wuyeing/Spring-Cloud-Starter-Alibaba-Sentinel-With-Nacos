package yh.springcloud.springcloudstarteralibabasentinelwithnacos;

import com.alibaba.cloud.sentinel.custom.SentinelAutoConfiguration;
import com.alibaba.cloud.sentinel.datasource.converter.JsonConverter;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import yh.springcloud.springcloudstarteralibabasentinelwithnacos.converter.Entity2RuleJsonConverter;
import yh.springcloud.springcloudstarteralibabasentinelwithnacos.utils.SpringBeanUtils;

import java.util.Map;

/**
 * <p>重新注册名为sentinel-json-authority-converter、sentinel-json-param-flow-converter的bean。</p>
 * <p>为了达到覆盖SentinelAutoConfiguration中对上述bean的定义，同时不被前者定义的bean覆盖，这里配置after确保在其之后执行</p>
 */
@AutoConfiguration(after = {SentinelAutoConfiguration.class})
@Import({SpringBeanUtils.class})
public class CustomSentinelAutoConfiguration {

    private ObjectMapper objectMapper = new ObjectMapper();

    public CustomSentinelAutoConfiguration() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
    }

    /**
     * 为了避免占用allow-bean-definition-overriding配置项，
     * 不使用spring原生的覆盖方式，而是基于@PostConstruct和spring容器工具类来覆盖bean
     */
    @PostConstruct
    void init() {
        Map<String, JsonConverter> map = Map.of(
                "sentinel-json-authority-converter",
                new Entity2RuleJsonConverter(new ObjectMapper(), AuthorityRule.class),
                "sentinel-json-param-flow-converter",
                new Entity2RuleJsonConverter(new ObjectMapper(), ParamFlowRule.class));
        map.forEach(SpringBeanUtils::registerSingletonWithOverwrite);
    }
}
