package com.chinaunicom.smartstorage.admin.controller.rbac;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionFindByNameReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionRespDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Region;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.RegionService;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

/**
 * @author liuchink
 * @since 2020/4/5 15:19
 */
@Tag(name = "系统管理-行政区域")
@RestController
@RequestMapping("/rbac/region")
@Slf4j
public class RegionController extends BaseController {
    @Resource
    private RegionService regionService;

    @Resource
    private UidGenerator uidGenerator;

    @Resource
    private MapperFacade mapperFacade;

    /**
     * 查询
     *
     * @param id id
     * @param levels levels
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    @Operation(summary = "行政区域级联查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listRegionChildByIdAndLevels/{id}/{levels}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<RegionRespDTO>, String> listOrganizationChildByIdAndLevels(
         @PathVariable String id,
        @PathVariable Integer levels) {
        List<Region> listRegion = regionService.listRegionChildByIdAndLevels(id, levels);
        List<RegionRespDTO> listRegionRespDTO = mapperFacade.mapAsList(listRegion, RegionRespDTO.class);
        return actionResultService.callBackResult(listRegionRespDTO);
    }

    /**
     * 查询
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    @Operation(summary = "根据ID查询所有子节点")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listRegionChildById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<RegionRespDTO>, String> listRegionChildById(
         @PathVariable String id) {
        List<Region> listRegion = regionService.listRegionChildById(id);
        List<RegionRespDTO> listRegionRespDTO = mapperFacade.mapAsList(listRegion, RegionRespDTO.class);
        return actionResultService.callBackResult(listRegionRespDTO);
    }

    /**
     * 查询
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    @Operation(summary = "根据ID查询所有父节点")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listRegionParentById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<RegionRespDTO>, String> listRegionParentById(
         @PathVariable String id) {
        List<Region> listRegion = regionService.listRegionParentById(id);
        List<RegionRespDTO> listRegionRespDTO = mapperFacade.mapAsList(listRegion, RegionRespDTO.class);
        return actionResultService.callBackResult(listRegionRespDTO);
    }

    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getRegionById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<RegionRespDTO, String>
        getRegionById( @PathVariable String id) {
        Region region = regionService.getRegionById(id);
        RegionRespDTO regionRespDTO = mapperFacade.map(region, RegionRespDTO.class);
        return actionResultService.callBackResult(regionRespDTO);
    }

    /**
     * 查询
     *
     * @param regionFindByNameReqDTO regionFindByNameReqDTO
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    @Operation(summary = "根据名称查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listRegionByName", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<RegionRespDTO>, String> listRegionByName(

        @RequestBody RegionFindByNameReqDTO regionFindByNameReqDTO) {
        List<Region> listRegion = regionService.listRegionByName(regionFindByNameReqDTO);
        List<RegionRespDTO> listRegionRespDTO = mapperFacade.mapAsList(listRegion, RegionRespDTO.class);
        return actionResultService.callBackResult(listRegionRespDTO);
    }

    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = RegionRespDTO.class))))
    @PostMapping(path = "/listRegionByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionRespDTO")
    public Object listRegionByConditionOnPage(
        @RequestBody @Validated RegionQueryReqDTO regionQueryReqDTO) {
        return regionService.listRegionByConditionOnPage(regionQueryReqDTO);
    }
}
